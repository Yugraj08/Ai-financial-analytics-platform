# Finance Backend RBAC System

## Project Overview
A secure backend system built using Spring Boot for managing financial records with JWT authentication and Role-Based Access Control (RBAC). The project provides secure REST APIs, filtering, pagination, dashboard analytics, and cloud deployment support.
## Tech Stack
- Java 17
- Spring Boot
- Spring Security
- JWT Authentication
- Spring Data JPA
- MySQL
- Docker
- Render
- Swagger / OpenAPI
## Features

- JWT-based Authentication & Authorization
- Role-Based Access Control (ADMIN, ANALYST, VIEWER)
- Secure Financial Record CRUD APIs
- Filtering & Pagination Support
- Dashboard Analytics (Income, Expense, Balance)
- User Role & Status Management
- Global Exception Handling
- Input Validation using Hibernate Validator
- Swagger API Documentation
- Cloud Deployment using Docker & Render


## API Endpoints

### Authentication APIs
- `POST /auth/register`
- `POST /auth/login`

### Record APIs
- `POST /records`
- `GET /records`
- `PUT /records/{id}`
- `DELETE /records/{id}`

### Dashboard API
- `GET /records/dashboard`

### User Management
- `PUT /users/{id}/role`
## Authentication Flow 
1. User logs in using `/auth/login`
2. Server returns JWT token
3. Token must be sent in request header:
4. Authorization: Bearer <token>
5. Spring Security validates token and role permissions
## Deployment
Live API

https://finance-backend-rbac.onrender.com

Swagger Documentation

https://finance-backend-rbac.onrender.com/swagger-ui/index.html
## Run Locally

Clone the project

```bash
  git clone https://github.com/Yugraj08/finance-backend-rbac.git
```

## Configure Environment Variables
DB_URL=your_database_url;

DB_USERNAME=your_username;

DB_PASSWORD=your_password;

JWT_SECRET=your_secret_key;



## Create MySQL Database
```bash
 CREATE DATABASE finance_backend;
```

## Run the Application

```bash
  mvn spring-boot:run
```

## Access Swagger UI

```bash
  http://localhost:8081/swagger-ui/index.html
```

## Environment Variables

To run this project, you will need to add the following environment variables to your .env file

`API_KEY`

`ANOTHER_API_KEY`


## Appendix

- JWT authentication is stateless

- BCrypt hashing is used for password security

- Protected APIs require a valid JWT token

- Free Render deployment may take a few seconds to wake up after inactivity

