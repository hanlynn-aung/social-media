d# Nightlife & Club Booking API

A comprehensive REST API for a Nightlife and Social Media application. This platform allows users to find bars/clubs, book tables, chat in groups, and see upcoming events.

## 🚀 Features

*   **User Management**: Sign Up, Sign In (JWT), Profile Management.
*   **Shops (Bars/Clubs)**: Search, Details, Location (Nearby Search), Ratings.
*   **Social Feed**: Shops can post announcements and events.
*   **Reservations**: Users can book tables (Payment status tracking).
*   **Real-time Chat**: Group chat for each shop using WebSockets & MongoDB.
*   **Notifications**: Real-time system and event notifications.

---

## 🛠 Prerequisites

*   **Java 17**
*   **Maven**
*   **Docker Desktop** (for the full demo experience)

---

## 📦 Installation & Running

### Option 1: Quick Start (Local H2 Database)
Best for quick testing without Docker. Data is lost on restart.

1.  **Clone/Open the project.**
2.  **Run the application:**
    *   **Windows**: Double-click `run.bat` or run `.\run.bat` in terminal.
    *   **Manual**: `mvn spring-boot:run`
3.  **Access**: `http://localhost:8080`
4.  **Database Console**: `http://localhost:8080/h2-console`
    *   JDBC URL: `jdbc:h2:mem:testdb`
    *   User: `sa`
    *   Password: `password`

### Option 2: Full Demo (Docker with Postgres & Mongo)
Best for client demos. Data is persisted.

1.  **Build the JAR:**
    ```bash
    mvn clean package -DskipTests
    ```
2.  **Start Environment:**
    ```bash
    docker compose up --build
    ```
3.  **Access**: `http://localhost:8080`
    *   **App**: Runs on port 8080.
    *   **PostgreSQL**: Port 5432 (User/Pass: `postgres`/`password`).
    *   **MongoDB**: Port 27017.

---

## 📝 Step-by-Step Usage Guide (Demo Script)

Follow these steps to test the full flow of the application using Postman or curl.

### 1. User Registration
Create a new user account.
*   **Endpoint**: `POST /api/auth/signup`
*   **Body**:
    ```json
    {
        "username": "hanlynn",
        "email": "hanlynn@example.com",
        "password": "password123",
        "phoneNumber": "1234567890",
        "role": "shop_admin"
    }
    ```

### 2. User Login
Login to get the JWT Token. You must include this token in the header of all subsequent requests (`Authorization: Bearer <token>`).
*   **Endpoint**: `POST /api/auth/signin`
*   **Body**:
    ```json
    {
        "username": "hanlynn",
        "password": "password123"
    }
    ```
*   **Response**: Copy the `token`.

### 3. Create a Shop (Club/Bar)
As the logged-in user (who is a `shop_admin`), create your shop.
*   **Endpoint**: `POST /api/shops/user/{userId}`
*   **Header**: `Authorization: Bearer <your_token>`
*   **Body**:
    ```json
    {
        "name": "Sky Bar Yangon",
        "description": "The best view in the city.",
        "address": "Sakura Tower, Yangon",
        "latitude": 16.7790,
        "longitude": 96.1578
    }
    ```

### 4. Post an Event
Announce a party at your shop.
*   **Endpoint**: `POST /api/shops/posts/shop/{shopId}`
*   **Body**:
    ```json
    {
        "content": "New Year Eve Party! 50% off on cocktails.",
        "type": "EVENT"
    }
    ```
    *(Note: This triggers a real-time notification to all users)*

### 5. Find Nearby Shops
Simulate a user searching for clubs near them.
*   **Endpoint**: `GET /api/shops/nearby?lat=16.7790&lng=96.1578&radius=5`

### 6. Make a Reservation
Book a table at the shop.
*   **Endpoint**: `POST /api/reservations/user/{userId}/shop/{shopId}`
*   **Body**:
    ```json
    {
        "reservationTime": "2023-12-31T20:00:00",
        "numberOfGuests": 4
    }
    ```

### 7. Send a Chat Message
Post a message in the shop's group chat.
*   **Endpoint**: `POST /api/messages/user/{userId}/shop/{shopId}`
*   **Body**:
    ```json
    {
        "content": "Is there a dress code tonight?"
    }
    ```

---

## 📡 API Reference

### Documentation (Swagger UI)
- URL: `http://localhost:8080/swagger-ui/index.html`
- Use this UI to explore endpoints and test the API interactively.
- To authenticate, click "Authorize" and paste your JWT token (Bearer <token>).

### Authentication
- `POST /api/auth/signin` - Login
- `POST /api/auth/signup` - Register (Roles: `user`, `shop_admin`, `admin`)
- `POST /api/auth/logout` - Logout

### Shops
- `GET /api/shops` - List all
- `GET /api/shops/search?name=xyz` - Search by name
- `GET /api/shops/nearby?lat=x&lng=y&radius=km` - Search by location
- `POST /api/shops/user/{id}` - Create Shop

### Reservations
- `POST /api/reservations/user/{uid}/shop/{sid}` - Create
- `PUT /api/reservations/{id}/payment?status=PAID` - Update payment
- `GET /api/reservations/user/{uid}` - List my reservations

### Chat & Notifications
- `POST /api/messages/user/{uid}/shop/{sid}` - Send Message
- `GET /api/messages/shop/{sid}` - History
- `GET /api/notifications/user/{uid}` - Get Notifications
- **WebSocket**: `ws://localhost:8080/ws`
