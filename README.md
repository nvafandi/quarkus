# Sales System - Quarkus Backend

A backend sales system built with Quarkus that manages Users, Products, and Transactions.

## Architecture
- **Framework:** Quarkus (Java 17)
- **Pattern:** Clean Architecture + Repository Pattern + Service Layer
- **Database:** PostgreSQL
- **Layers:** Resource (REST API) → Service (Business Logic) → Repository (Data Access) → Entity (JPA)

## Tech Stack
- Quarkus 3.6.0
- Java 17
- PostgreSQL
- Hibernate ORM with Panache
- RESTEasy Reactive + JSON-B
- Bean Validation

## Prerequisites
- Java 17+
- Maven 3.9+
- PostgreSQL 14+

## Database Setup

```sql
CREATE DATABASE sales_db;
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
quarkus.datasource.username=your_username
quarkus.datasource.password=your_password
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/sales_db
```

## Running the Application

### Development Mode
```bash
./mvnw quarkus:dev
```

### Build
```bash
./mvnw clean package
```

### Run JAR
```bash
java -jar target/quarkus-app/quarkus-run.jar
```

### Native Build
```bash
./mvnw package -Pnative
```

## API Endpoints

### Users
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/users` | Get all users |
| GET | `/api/users/{id}` | Get user by ID |
| POST | `/api/users` | Create user |
| PUT | `/api/users/{id}` | Update user |
| DELETE | `/api/users/{id}` | Delete user |

### Products
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/products` | Get all products |
| GET | `/api/products/{id}` | Get product by ID |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

### Transactions
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/transactions` | Get all transactions |
| GET | `/api/transactions/{id}` | Get transaction by ID |
| GET | `/api/transactions/user/{userId}` | Get transactions by user |
| POST | `/api/transactions` | Create transaction |
| DELETE | `/api/transactions/{id}` | Delete transaction |

## Example Requests

### Create User
```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{"username":"john","password":"secret123","role":"CASHIER"}'
```

### Create Product
```bash
curl -X POST http://localhost:8080/api/products \
  -H "Content-Type: application/json" \
  -d '{"name":"Laptop","price":1500000,"stock":50}'
```

### Create Transaction
```bash
curl -X POST http://localhost:8080/api/transactions \
  -H "Content-Type: application/json" \
  -d '{
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "items": [
      {"productId": "c3d4e5f6-a7b8-9012-cdef-123456789012", "quantity": 2}
    ]
  }'
```

## Project Structure

```
sales-system/
├── src/main/java/com/sales/
│   ├── entity/
│   │   ├── UserEntity.java
│   │   ├── ProductEntity.java
│   │   ├── TransactionEntity.java
│   │   └── TransactionItemEntity.java
│   ├── repository/
│   │   ├── UserRepository.java
│   │   ├── ProductRepository.java
│   │   └── TransactionRepository.java
│   ├── service/
│   │   ├── UserService.java
│   │   ├── ProductService.java
│   │   └── TransactionService.java
│   ├── resource/
│   │   ├── UserResource.java
│   │   ├── ProductResource.java
│   │   ├── TransactionResource.java
│   │   └── GlobalExceptionMapper.java
│   └── dto/
│       ├── UserDTO.java
│       ├── ProductDTO.java
│       ├── TransactionDTO.java
│       ├── TransactionItemDTO.java
│       └── ErrorResponse.java
├── src/main/resources/
│   ├── application.properties
│   └── import.sql
└── src/test/java/com/sales/
    └── UserResourceTest.java
```

## Notes
- All primary keys use UUID
- Stock validation before creating transaction
- Transaction management in service layer
- DTO pattern for API communication
- Global exception handling
