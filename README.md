# Shopping Backend Project

## Overview

This is a microservices-based e-commerce backend system built with Spring Boot. The system is designed to handle shopping functionalities through separate services that communicate via Kafka messaging.

### Key Features

- **Microservices Architecture**: Separate services for accounts, items, orders, and payments. Each microservice has its own dedicated database
- **Event-Driven**: Using Kafka for asynchronous communication between services
- **Multiple Databases**: Different databases optimized for each service's needs
- **JWT Authentication**: Secure API endpoints with JWT tokens
- **Idempotency**: Handling duplicate requests safely

### Services Architecture

- **Account Service**: Handles user authentication and management (MySQL)
- **Item Service**: Manages product inventory (MongoDB)
- **Order Service**: Processes customer orders (Cassandra)
- **Payment Service**: Handles payment processing (MySQL)
- **Common Service**: Shared utilities and configurations

### Technology Stack

- Java 17
- Spring Boot 3.2.0
- Apache Kafka
- MySQL 8
- MongoDB
- Apache Cassandra
- Docker & Docker Compose
- Maven

## Getting Started

### Prerequisites

- JDK 17
- Maven
- Docker & Docker Compose (for databases and Kafka)
- Git
- IDE (IntelliJ IDEA recommended)

### Running Locally

1. Clone the Repository

```bash
git clone https://github.com/XUANL19/shopping-backend.git
cd shopping-backend
```

2. Start Infrastructure Services

```bash
docker-compose up -d account-mysql payment-mysql mongodb cassandra kafka zookeeper
```

3. Import Project into IDE

- Open IntelliJ IDEA
- File -> Open -> Select the project directory
- Wait for Maven to download dependencies

4. Build the Project

```bash
mvn clean install
```

5. Run Each Service
   In IntelliJ IDEA:

- Navigate to each service's Application class:
  - AccountServiceApplication
  - ItemServiceApplication
  - OrderServiceApplication
  - PaymentServiceApplication
- Right-click and select "Run"

Or via command line in separate terminals:

```bash
# Account Service
cd account-service
mvn spring-boot:run

# Item Service
cd item-service
mvn spring-boot:run

# Order Service
cd order-service
mvn spring-boot:run

# Payment Service
cd payment-service
mvn spring-boot:run
```

The services will be available at:

- Account Service: http://localhost:8081
- Item Service: http://localhost:8082
- Order Service: http://localhost:8083
- Payment Service: http://localhost:8084

## Database Schemas

### Account Service (MySQL)

```json
{
  "id": "UUID",
  "email": "string",
  "password": "string (hashed)"
}
```

### Item Service (MongoDB)

```json
{
  "id": "string",
  "upc": "long",
  "itemName": "string",
  "price": "double",
  "purchaseLimit": "integer",
  "category": "string",
  "inventory": "integer"
}
```

### Order Service (Cassandra)

```json
{
  "orderId": "UUID",
  "userId": "UUID",
  "orderStatus": "string",
  "items": [
    {
      "upc": "long",
      "purchaseCount": "integer"
    }
  ],
  "createdAt": "timestamp",
  "updatedAt": "timestamp",
  "idempotencyKey": "string"
}
```

### Payment Service (MySQL)

```json
{
  "paymentId": "UUID",
  "orderId": "UUID",
  "userId": "UUID",
  "paymentCard": "string",
  "expiration": "string",
  "cvv": "string",
  "billingAddress": "string",
  "zip": "string",
  "paymentStatus": "string",
  "idempotencyKey": "string",
  "createdAt": "timestamp",
  "updatedAt": "timestamp"
}
```

## Event-Driven Architecture

### Order Status Updates (Topic: order-status-updates)

Order Service sends messages about order status changes:

```json
{
    "orderId": "UUID",
    "userId": "UUID",
    "orderStatus": "PAID / USER_CANCELED",
    "items": [
        {
            "upc": "long",
            "purchaseCount": "integer"
        }
    ]
}
```

#### Consumers:

1. **Item Service**
    - Listens for: "PAID" status
    - Action: Decreases inventory for each item in the order

2. **Payment Service**
    - Listens for: "USER_CANCELED" status
    - Action: Updates payment status to "User Canceled"

### Payment Status Updates (Topic: payment-status-updates)

Payment Service sends messages about payment status changes:

1. **Payment Successful**
```json
{
    "orderId": "UUID",
    "status": "PAID"
    "reason": "Payment Successful"
}
```

2. **Payment Failed**
```json
{
    "orderId": "UUID",
    "status": "PAYMENT_FAILED",
    "reason": "Insufficient Funds"
}
```

3. **Payment Cancelled**
```json
{
    "orderId": "UUID",
    "status": "CANCELLED",
    "reason": "Fraudulent Transaction"
}
```

4. **Payment Needs Retry**
```json
{
    "orderId": "UUID",
    "status": "REPAY_NEEDED",
    "reason": "Chargeback Initiated"
}
```

#### Consumers:

1. **Order Service**
    - Updates order status based on payment status:
        - "Payment Successful" → Order status: "PAID"
        - "Insufficient Funds" → Order status: "PAYMENT_FAILED"
        - "Fraudulent Transaction" → Order status: "CANCELLED"
        - "Chargeback Initiated" → Order status: "REPAY_NEEDED"

### Event Processing Logic

1. **Payment Status Updates**

   - Payment Service sends payment status updates to topic "payment-status-updates"
   - Order Service consumes these updates and updates order status
   - Payment status probabilities:
     - "Payment Successful": 40%
     - "Insufficient Funds": 20%
     - "Fraudulent Transaction": 20%
     - "Chargeback Initiated": 20%

## API Testing by Postman

Import file `Shopping Backend.postman_collection` in project root path within Postman to test all APIs.