package hng_java_boilerplate.payment.service;


import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Product;
import com.stripe.model.Price;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import hng_java_boilerplate.payment.dtos.PaymentNotFoundException;
import hng_java_boilerplate.payment.dtos.PaymentRequestBody;
import hng_java_boilerplate.payment.dtos.SessionResponse;
import hng_java_boilerplate.payment.entity.Payment;
import hng_java_boilerplate.payment.enums.PaymentStatus;
import hng_java_boilerplate.payment.repository.PaymentRepository;
import hng_java_boilerplate.payment.utils.CustomerUtils;
import hng_java_boilerplate.plans.entity.Plan;
import hng_java_boilerplate.plans.service.PlanService;
import hng_java_boilerplate.user.entity.User;
import hng_java_boilerplate.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

import static com.stripe.param.checkout.SessionCreateParams.*;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Logger logger = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repository;
    private final PlanService planService;
    private final UserService userService;

    @Value("${stripe.api.key}")
    private String API_KEY;

    @Value("${client.url}")
    private String clientBaseUrl;

    @Value("${stripe.secret.key}")
    private String API_SECRET;

    public ResponseEntity<SessionResponse> createSession(PaymentRequestBody todo) throws StripeException {
        Plan plan = planService.findOne(todo.planId());
        User loggedUser = userService.getLoggedInUser();
        Stripe.apiKey = API_KEY;


        Payment payment = new Payment();
        payment.setAmount(plan.getPrice());
        payment.setStatus(PaymentStatus.SUCCESS);
        Set<User> users = payment.getUser();
        if (users == null) {
            users = new HashSet<>();
        }
        users.add(loggedUser);
        payment.setUser(users);

        Payment saved = repository.save(payment);

        Map<String, String> metadata = new HashMap<>() {{
            put("product_id", plan.getId());
            put("user_id", loggedUser.getId());
            put("payment_id", saved.getId());
        }};

        Product product = new Product();
        Price price = new Price();

        price.setCurrency("usd");
        price.setUnitAmountDecimal(BigDecimal.valueOf(plan.getPrice() * 100));
        product.setName(plan.getName() + " pricing plan");
        product.setId(plan.getId());
        product.setDefaultPriceObject(price);

        Customer customer = CustomerUtils.findOrCreateCustomer(loggedUser.getEmail(), loggedUser.getName());

        Builder params;
        params = builder()
                .setMode(Mode.PAYMENT)
                .setCustomer(customer.getId())
                .setSuccessUrl(clientBaseUrl + "/session_id=" + payment.getId())
                .setCancelUrl(clientBaseUrl + "/")
                .putAllMetadata(metadata)
                .addLineItem(LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(
                                LineItem.PriceData.builder()
                                        .setProductData(
                                                LineItem.PriceData.ProductData.builder()
                                                        .setName(product.getName())
                                                        .build()
                                        )
                                        .setCurrency(product.getDefaultPriceObject().getCurrency())
                                        .setUnitAmountDecimal(product.getDefaultPriceObject().getUnitAmountDecimal())
                                        .build()
                        )
                        .build()
                );
        Session session = Session.create(params.build());
        return ResponseEntity.ok(new SessionResponse(session.getUrl()));
    }

    public ResponseEntity<?> returnStatus(String id) {
        Optional<Payment> payment = repository.findById(id);

        if (payment.isEmpty()) {
            throw new PaymentNotFoundException("Payment not found");
        }
        return ResponseEntity.ok(new HashMap<>(){{
            put("status", payment.get().getStatus());
        }});
    }
}
