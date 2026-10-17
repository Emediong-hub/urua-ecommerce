# Uruà – E-Commerce Backend

Uruà is an e-commerce platform that helps small businesses sell to buyers across all 36 states of Nigeria. This repository is the REST API. It focuses on trust and convenience in online buying: payments are verified with Paystack before an order counts as paid, and the **buyer** closes each order by confirming delivery.

The React frontend lives in a separate repository: https://github.com/Emediong-hub/urua-frontend

## Author

Michael Emediong – Web/Java track

## Features

- **Accounts:** register and log in as a buyer or a seller. Passwords are hashed with BCrypt, and logins use JWT tokens that last 24 hours.
- **Product catalog:** sellers list products. Anyone can browse and search by name, category, state and price range, with pagination.
- **Shopping cart:** buyers add items, change quantities and remove items. Prices and stock are looked up fresh every time, so the cart never shows a stale price.
- **Checkout and payment:** the cart becomes an order with the prices frozen at purchase time. Stock is reserved safely, so two buyers can't take the last item. Payment goes through Paystack.
- **Payment verification:** the server asks Paystack directly whether the payment happened and checks that the amount matches exactly before marking the order as paid.
- **Order tracking:** `PENDING_PAYMENT` → `PAID` → `SHIPPED` → `DELIVERED` (or `CANCELLED`). The seller marks an order as shipped, and the buyer confirms delivery.
- **Unpaid-order cleanup:** a scheduled job checks orders that stay unpaid for 30 minutes. If Paystack shows the payment was abandoned or failed, the order is cancelled and its stock goes back on sale. If the buyer actually paid but never returned to the site, the order is completed as paid.

## Tech stack

- Java 21, Spring Boot 4.1
- Spring Web, Spring Security (JWT with jjwt), Spring Validation
- Spring Data MongoDB
- Paystack (test mode)
- Maven (wrapper included), Lombok
- JUnit 5 and Mockito for tests

## Project structure

```
src/main/java/com/urua/urua_backend
├── config        Security (JWT filter, CORS, roles) and scheduling setup
├── controller    REST endpoints (they only pass work to the services)
├── service       Business logic
├── repository    MongoDB access
├── model         Stored documents (User, Product, Cart, Order ...)
├── dto           Request and response objects
└── security      JWT authentication filter
```

## API overview

| Method | Endpoint | Who | Purpose |
|--------|----------|-----|---------|
| GET | `/api/health` | Public | Status check |
| POST | `/api/auth/register` | Public | Create a buyer or seller account |
| POST | `/api/auth/login` | Public | Log in and receive a token |
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

### Steps

1. Clone the repository:

```
git clone https://github.com/Emediong-hub/urua-ecommerce.git
cd urua-ecommerce
```

2. Create the file `src/main/resources/application-local.properties` and put your Paystack test key in it:

```
app.paystack.secret-key=sk_test_your_key_here
```

This file is listed in `.gitignore`, so the key is never uploaded. Without it the app still starts, but starting a payment fails with a 502 error.

3. Start the app:

```
.\mvnw spring-boot:run
```

On macOS or Linux, use `./mvnw spring-boot:run`. The API runs at `http://localhost:8080`, and `http://localhost:8080/api/health` should show `UP`.

### Tests

```
.\mvnw clean test
```

The tests use Mockito, so they don't need MongoDB or internet.

### Build the JAR

```
.\mvnw clean install
java -jar target\urua-backend-0.0.1-SNAPSHOT.jar
```

The JAR includes your local properties file, so don't share it.

### Demo script

With the app running, this script registers a fresh seller and buyer, lists a product, adds it to the cart, checks out (you pay on Paystack's test page), and takes the order through shipping and delivery:

```
powershell -ExecutionPolicy Bypass -File .\demo.ps1
```

## Configuration

| Setting | Default | Notes |
|---------|---------|-------|
| `spring.mongodb.uri` | `mongodb://localhost:27017/urua` | Database connection |
| `app.paystack.secret-key` | none | Put it in `application-local.properties` |
| `app.paystack.callback-url` | `http://localhost:5173/payment/callback` | Where Paystack sends the buyer after paying |
| `app.jwt.secret` | development value | **Replace before deploying** |
| `app.jwt.expiration-ms` | `86400000` (24 hours) | Token lifetime |
| `app.shipping.flat-fee` | `0` | Flat delivery fee in naira |
| `app.orders.expiry-minutes` | `30` | How long an order may stay unpaid |
| `app.orders.cleanup-interval-ms` | `300000` (5 minutes) | How often the cleanup job runs |

The API accepts browser requests from `http://localhost:5173`, which is where the frontend runs in development.

## Roadmap

- Admin screen to verify sellers (a `verified` flag already exists on users)
- One shipment per seller when a cart mixes products from several sellers
- Image uploads (products currently use image links)
- More automated tests for the cart, checkout and fulfillment
- Deployment settings: secrets from environment variables, and the real frontend address for CORS