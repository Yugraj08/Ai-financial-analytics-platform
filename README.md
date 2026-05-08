# Finance Data Processing & Access Control Backend

## Project Description

This project is a backend system built using **Spring Boot** for managing financial records securely.
It implements **JWT-based authentication**, **role-based access control (RBAC)**, filtering, pagination, and dashboard analytics.

The system ensures that users can only access data based on their roles and ownership.

---

## Tech Stack

* Java 17
* Spring Boot
* Spring Security (JWT)
* Spring Data JPA
* MySQL
* Lombok

---

##  Features

* User Registration & Login (JWT Authentication)
* Role-Based Access Control (ADMIN, ANALYST, VIEWER)
* User Status Management (ACTIVE / INACTIVE)
* Secure Record CRUD APIs
* Filtering using Specification API
* Pagination support
* Dashboard (Income, Expense, Balance, Category Summary)
* Global Exception Handling
* Input Validation using Hibernate Validator

---

##  Setup Instructions

1. Clone the repository
2. Open the project in IntelliJ IDEA
3. Configure MySQL database in `application.properties`
4. Run the Spring Boot application
5. Use Postman to test APIs

---

##  Authentication Flow

1. User logs in using `/auth/login`
2. Server returns a JWT token
3. Token must be sent in header:

   ```
   Authorization: Bearer <token>
   ```
4. JwtFilter extracts userId and role from token
5. Spring Security enforces access using roles

---

## API Endpoints

### 🔹 Auth APIs

* `POST /auth/register` → Register user
* `POST /auth/login` → Login & get JWT

---

### 🔹 Record APIs

* `POST /records` → Create record
* `GET /records` → Get records (with filters & pagination)
* `PUT /records/{id}` → Update record
* `DELETE /records/{id}` → Delete record

---

### 🔹 Dashboard API

* `GET /records/dashboard` → Get summary (income, expense, balance, category)

---

### 🔹 User Management

* `PUT /users/{id}/role` → Update user role (ADMIN only)

---

##  RBAC (Role-Based Access Control)

| Role    | Permissions      |
| ------- | ---------------- |
| ADMIN   | Full access      |
| ANALYST | Read + dashboard |
| VIEWER  | Read only        |

---

##  Assumptions

* Role is not assigned during registration for security reasons
* JWT is stateless, so role updates require re-login
* Passwords are stored securely using BCrypt hashing
* Only authenticated users can access protected APIs

---

## ️ Trade-offs

* Used JWT for stateless authentication instead of session-based approach
* Used Specification API for flexible filtering instead of fixed queries
* Focused on backend implementation instead of building a frontend
* Did not include Swagger documentation to keep project lightweight

---

## API Testing (Postman)

The Postman collection for all APIs is included in this repository.

### How to use:
1. Open Postman
2. Click Import
3. Select the file: `finance-backend-postman-collection.json`
4. Test all endpoints

Note:
- Base URL: http://localhost:8081
- Use JWT token from login API for secured endpoints

---

##  Important Notes

* After updating user role, user must login again to get updated permissions
* Unauthorized access is restricted using Spring Security annotations
* Proper exception handling is implemented for better API responses

---

## Author

Yugraj Mewara
