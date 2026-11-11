# Uruà – E-Commerce Backend

Uruà is an e-commerce platform that helps small businesses sell to buyers across all 36 states of Nigeria. This repository is the REST API. It focuses on trust and convenience in online buying: accounts are confirmed by email, payments are verified with Paystack before an order counts as paid, and the **buyer** closes each order by confirming delivery.

The React frontend lives in a separate repository: https://github.com/Emediong-hub/urua-frontend

## Author

Michael Emediong – Web/Java track

## Features

- **Accounts:** register and log in as a buyer or a seller. Passwords are hashed with BCrypt, and logins use JWT tokens that last 24 hours.
- **Email verification:** after registering, the user receives an email with a one-time link. Only a hash of the token is stored, the link expires after 24 hours, and it works once. Login is refused until the email is verified, and a new link can be requested once a minute.
- **Product catalog:** sellers list products. Anyone can browse and search by name, category, state and price range, with pagination.
- **Shopping cart:** buyers add items, change quantities and remove items. Prices and stock are looked up fresh every time, so the cart never shows a stale price.
- **Checkout and payment:** the cart becomes an order with the prices frozen at purchase time. Stock is reserved safely, so two buyers can't take the last item. Payment goes through Paystack.
- **Payment verification:** the server asks Paystack directly whether the payment happened and checks that the amount matches exactly before marking the order as paid.
- **Order tracking:** `PENDING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED` (or `CANCELLED`). The seller marks an order as shipped, and the buyer confirms delivery.
- **Unpaid-order cleanup:** a scheduled job checks orders that stay unpaid for 30 minutes. If Paystack shows the payment was abandoned or failed, the order is cancelled and its stock goes back on sale. If the buyer actually paid but never returned to the site, the order is completed as paid.

## Tech stack

- Java 21, Spring Boot 4.1
- Spring Web, Spring Security (JWT with jjwt), Spring Validation, Spring Mail
- Spring Data MongoDB
- Paystack (test mode)
- Maven (wrapper included), Lombok
- JUnit 5 and Mockito for tests

## Project structure

```
src/main/java/com/urua/urua_backend
├── config        Security (JWT filter, CORS, roles) and scheduling setup
├── controller    REST endpoints (they only pass work to the services)
├── service       Business logic, including email and Paystack
├── repository    MongoDB access
├── model         Stored documents (User, Product, Cart, Order ...)
├── dto           Request and response objects
└── security      JWT authentication filter
```

## API overview

| Method | Endpoint | Who | Purpose |
|--------|----------|-----|---------|
| GET | `/api/health` | Public | Status check |
| POST | `/api/auth/register` | Public | Create a buyer or seller account and send the verification email |
| POST | `/api/auth/verify-email` | Public | Confirm an email address with the token from the email |
| POST | `/api/auth/resend-verification` | Public | Send a new verification email (once per minute) |
| POST | `/api/auth/login` | Public | Log in and receive a token (403 until the email is verified) |
| GET | `/api/users/me` | Logged in | Current user |
| GET | `/api/products` | Public | Browse and search (`q`, `category`, `state`, `minPrice`, `maxPrice`, `page`, `size`) |
| GET | `/api/products/{id}` | Public | One product |
| POST | `/api/products` | Seller | List a product |
| GET | `/api/products/mine` | Seller | The seller's own products |
| GET | `/api/cart` | Buyer | View the cart |
| POST | `/api/cart/items` | Buyer | Add an item |
| PUT | `/api/cart/items` | Buyer | Set an item's quantity |
| DELETE | `/api/cart/items/{productId}` | Buyer | Remove an item |
| DELETE | `/api/cart` | Buyer | Empty the cart |
| POST | `/api/orders/checkout` | Buyer | Create an order and get the Paystack payment link |
| POST | `/api/orders/verify/{reference}` | Buyer | Verify the payment with Paystack |
| GET | `/api/orders` | Buyer | The buyer's orders |
| GET | `/api/orders/{id}` | Buyer | One order |
| POST | `/api/orders/{id}/confirm-delivery` | Buyer | Confirm the goods arrived |
| GET | `/api/seller/orders` | Seller | Paid orders containing the seller's products |
| PUT | `/api/seller/orders/{id}/ship` | Seller | Mark an order as shipped |

Send the token as `Authorization: Bearer <token>`.

## Running it locally

### Requirements

- JDK 21 or newer
- MongoDB running on `localhost:27017`
- A free Paystack account, using the **Test Secret Key** (starts with `sk_test_`)
- A Gmail account with an **app password**, used to send verification emails

### Steps

1. Clone the repository:

```
git clone https://github.com/Emediong-hub/urua-ecommerce.git
cd urua-ecommerce
```

2. Create the file `src/main/resources/application-local.properties`. It is listed in `.gitignore`, so your secrets are never uploaded. Put your Paystack key and mail login in it:

```
app.paystack.secret-key=sk_test_your_key_here
spring.mail.username=youraddress@gmail.com
spring.mail.password=your16characterapppassword
```

To get a Gmail app password, turn on 2-Step Verification for the account at https://myaccount.google.com/security, then create an app password at https://myaccount.google.com/apppasswords. Paste it without spaces, and never commit it.

The app still starts without these lines. Without the Paystack key, starting a payment fails with a 502 error. Without the mail login, verification emails can't be sent, so new accounts can't log in until they are verified.

3. Start the app:

```
.\mvnw spring-boot:run
```

On macOS or Linux, use `./mvnw spring-boot:run`. The API runs at `http://localhost:8080`, and `http://localhost:8080/api/health` should show `UP`.

### Email notes

- The settings use Gmail's SMTP server on port 465 (SSL). Some networks block port 587, which is why 465 is used.
- Verification links point to the frontend (`app.frontend-url`), so they open correctly on the computer running the project.
- Emails to `@example.com` addresses are skipped, since that domain is reserved for demos and tests.
- When testing with one Gmail account, `yourname+a1@gmail.com` counts as a new address to Uruà but still arrives in your inbox.

### Tests

```
.\mvnw clean test
```

There are 18 tests covering registration, login, token security and email verification (hashing, expiry, single use and the resend limit). They use Mockito, so they need neither MongoDB nor internet.

### Build the JAR

```
.\mvnw clean install
java -jar target\urua-backend-0.0.1-SNAPSHOT.jar
```

The JAR includes your local properties file, so don't share it.

### Demo script

With the app and MongoDB running, this script registers a fresh seller and a buyer, shows that login is refused before verification, marks the two demo accounts verified, lists a product, adds it to the cart, checks out (you pay on Paystack's test page), and takes the order through shipping and delivery:

```
powershell -ExecutionPolicy Bypass -File .\demo.ps1
```

It uses reserved `example.com` addresses, which have no inbox, so it verifies them directly in the database with `mongosh`. `mongosh` must be installed and on your PATH.

## Configuration

| Setting | Default | Notes |
|---------|---------|-------|
| `spring.mongodb.uri` | `mongodb://localhost:27017/urua` | Database connection |
| `app.paystack.secret-key` | none | Put it in `application-local.properties` |
| `app.paystack.callback-url` | `http://localhost:5173/payment/callback` | Where Paystack sends the buyer after paying |
| `spring.mail.host` and `spring.mail.port` | `smtp.gmail.com` and `465` | Mail server (SSL) |
| `spring.mail.username` and `spring.mail.password` | none | Put them in `application-local.properties` |
| `app.frontend-url` | `http://localhost:5173` | Base address used in the verification link |
| `app.mail.from-name` | `Uruà` | Sender name shown in emails |
| `app.jwt.secret` | development value | **Replace before deploying** |
| `app.jwt.expiration-ms` | `86400000` (24 hours) | Token lifetime |
| `app.shipping.flat-fee` | `0` | Flat delivery fee in naira |
| `app.orders.expiry-minutes` | `30` | How long an order may stay unpaid |
| `app.orders.cleanup-interval-ms` | `300000` (5 minutes) | How often the cleanup job runs |

The API accepts browser requests from `http://localhost:5173`, which is where the frontend runs in development.

## Roadmap

- Deployment: secrets from environment variables, the real frontend address for CORS and email links, and an email service that sends over HTTPS with its own sender domain
- Admin screen to verify sellers (a `verified` flag already exists on users, separate from email verification)
- One shipment per seller when a cart mixes products from several sellers
- Image uploads (products currently use image links)
- More automated tests for the cart, checkout and fulfillment