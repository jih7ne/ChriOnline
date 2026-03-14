# ChriOnline — Java E-Commerce Desktop Application

> A client-server e-commerce desktop application built with **Java 21**, **JavaFX 17**, and **MySQL**, using a custom TCP/IP communication protocol.

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Features](#features)
4. [Tech Stack](#tech-stack)
5. [Project Structure](#project-structure)
6. [Database Schema](#database-schema)
7. [Prerequisites](#prerequisites)
8. [Getting Started](#getting-started)
9. [Configuration](#configuration)
10. [Running the Application](#running-the-application)
11. [Network Protocol](#network-protocol)
12. [API Reference (Controllers)](#api-reference-controllers)

---

## Overview

**ChriOnline** is a fully functional desktop e-commerce platform composed of two separate runnable programs:

- **Server** — accepts TCP connections, processes business logic, and communicates with the MySQL database.
- **Client** — a JavaFX graphical application that connects to the server and provides the user interface for shopping, cart management, checkout, payment, and user account management.

The two programs communicate via a **custom JSON-over-TCP protocol** on port `5000`.

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   CLIENT (JavaFX)                   │
│  LoginView ▸ CatalogueView ▸ PanierView ▸ ...       │
│              TCPClient (port 5000)                  │
└─────────────────────┬───────────────────────────────┘
                      │  JSON over TCP
┌─────────────────────▼───────────────────────────────┐
│                    SERVER                           │
│  TCPServer ▸ ClientHandler ▸ RequestDispatcher      │
│       ▸ Controllers ▸ Services ▸ Repositories       │
│                   MySQL DB                          │
└─────────────────────────────────────────────────────┘
```

The architecture follows a layered pattern:

| Layer        | Package                  | Role                                           |
|--------------|--------------------------|------------------------------------------------|
| **Network**  | `network.tcp`            | TCP Server / Client sockets                    |
| **Protocol** | `network.protocol`       | `AppRequest` / `AppResponse` (JSON serialized) |
| **Dispatch** | `network`                | `RequestDispatcher` routes requests to controllers |
| **Controller** | `server.controllers`   | Handle incoming requests and return responses  |
| **Service**  | `server.services`        | Business logic                                 |
| **Repository** | `server.repositories`  | Data access layer (JDBC + MySQL)               |
| **Model**    | `server.data.models`     | Domain objects (Produit, Commande, etc.)       |
| **UI (Client)** | `client.ui.views`     | JavaFX views rendered on the client side       |
| **Config**   | `core.config`            | Centralized DI container (`AppConfig`)         |

---

## Features

### 👤 Authentication
- User registration with email validation and password hashing (SHA-256)
- Login / Logout with UUID-based session tokens (stored server-side in memory)
- Profile viewing and updating
- Password change with old-password verification

### 🛍️ Product Catalog
- Browse products with images, prices, and categories
- View product details
- Filter and search products by category

### 🛒 Shopping Cart
- Add / remove products
- Update quantities
- View cart summary with total price

### 💳 Checkout & Payment
- Multi-step checkout: address selection → payment method → confirmation
- Supported payment methods: `carte_bancaire`, `paypal`, `fictif`
- Order creation linked to cart items
- Payment record saved and linked to order

### 📦 Orders & History
- Track order status: `en_attente`, `validée`, `expédiée`, `livrée`, `annulée`
- Order confirmation screen after payment

### 📍 Address Management
- Add, edit, and select delivery addresses per user

### 🔔 Notifications
- System notifications linked to orders (read/unread status)

### 🛠️ Admin Panel
- List all registered users
- Block / Unblock user accounts
- Delete users
- Product management (add, update, delete, stock management)

---

## Tech Stack

| Technology        | Version   | Purpose                         |
|-------------------|-----------|---------------------------------|
| Java              | 21        | Core language                   |
| JavaFX            | 17.0.6    | Desktop UI framework            |
| Maven             | 3.x       | Build tool & dependency manager |
| MySQL             | 8+        | Relational database             |
| MySQL Connector/J | 9.0.0     | JDBC driver                     |
| Gson              | 2.11.0    | JSON serialization              |
| SLF4J + Logback   | 2.0 / 1.5 | Logging                         |
| ControlsFX        | 11.2.1    | Extra JavaFX controls           |
| Ikonli (Feather)  | 12.3.1    | Icon pack for JavaFX            |
| TilesFX           | 21.0.3    | Dashboard tiles for JavaFX      |
| JUnit Jupiter     | 5.10.2    | Unit testing                    |

---

## Project Structure

```
ChriOnline/
├── pom.xml                          # Maven build configuration
├── ecommerce.sql                    # Full DB schema (run this first)
├── add_address_table.sql            # Migration: address table
├── add_numero_masque_column.sql     # Migration: masked card number
├── add_prix_unitaire_column.sql     # Migration: unit price column
├── add_uuid_commande_column.sql     # Migration: UUID for orders
├── test_data.sql                    # Sample data for testing
└── src/
    └── main/
        ├── resources/
        │   ├── application.properties   # DB connection config
        │   └── logback.xml              # Logging config
        └── java/com/chrionline/chrionline/
            ├── client/
            │   ├── HelloApplication.java            # JavaFX entry point
            │   ├── ClientApplication.java           # Main client app & ViewManager
            │   ├── controllers/
            │   │   └── MainController.java
            │   └── ui/
            │       ├── views/
            │       │   ├── LoginView.java
            │       │   ├── RegisterView.java
            │       │   ├── CatalogueView.java
            │       │   ├── DetailsProduitView.java
            │       │   ├── PanierView.java
            │       │   ├── CheckoutView.java
            │       │   ├── ConfirmationView.java
            │       │   ├── AdresseDialogView.java
            │       │   ├── AdminView.java
            │       │   ├── CommandeView.java
            │       │   └── HistoriqueView.java
            │       └── components/
            ├── server/
            │   ├── ServerApplication.java           # Server entry point
            │   ├── controllers/
            │   │   ├── AuthController.java          # Login, register, sessions
            │   │   ├── ProduitController.java       # Product CRUD
            │   │   ├── PanierController.java        # Cart management
            │   │   ├── CommandeController.java      # Orders
            │   │   ├── PaiementController.java      # Payments
            │   │   ├── AdresseController.java       # Delivery addresses
            │   │   └── AdminController.java         # Admin actions
            │   ├── services/
            │   │   ├── ProduitService.java
            │   │   ├── PanierService.java
            │   │   ├── CommandeService.java
            │   │   ├── PaiementService.java
            │   │   └── AdresseService.java
            │   ├── repositories/
            │   │   ├── UtilisateurRepository.java
            │   │   ├── ProduitRepository.java
            │   │   ├── PanierRepository.java
            │   │   ├── CommandeRepository.java
            │   │   ├── LigneCommandeRepository.java
            │   │   ├── PaiementRepository.java
            │   │   └── AdresseRepository.java
            │   └── data/
            │       ├── models/                      # Domain entities
            │       ├── dto/                         # Data Transfer Objects
            │       └── mappers/                     # RowMappers for JDBC
            ├── network/
            │   ├── tcp/
            │   │   ├── TCPServer.java               # Accepts incoming connections
            │   │   └── TCPClient.java               # Sends requests to server
            │   ├── protocol/
            │   │   ├── AppRequest.java              # Request model (JSON)
            │   │   └── AppResponse.java             # Response model (JSON)
            │   ├── ClientHandler.java               # Per-client thread
            │   └── RequestDispatcher.java           # Routes requests to controllers
            └── core/
                ├── config/
                │   ├── AppConfig.java               # DI container + DB connection
                │   └── AppConstants.java            # Global constants
                ├── interfaces/
                │   ├── IController.java
                │   └── ViewManager.java
                ├── utils/
                │   └── JsonUtils.java
                ├── enums/
                ├── exceptions/
                └── theme/
```

---

## Database Schema

The database is named `ecommerce`. Run the main schema file first, then apply migrations in order.

| Table             | Description                                |
|-------------------|--------------------------------------------|
| `Utilisateur`     | Users (client / admin roles)               |
| `Categorie`       | Product categories                         |
| `Produit`         | Products with price, stock, image URL      |
| `Panier`          | Shopping carts (one per user session)      |
| `Produit_Panier`  | Items in a cart (quantity per product)     |
| `Commande`        | Orders placed by users                     |
| `Ligne_Commande`  | Individual line items within an order      |
| `Paiement`        | Payment records linked to orders           |
| `Notification`    | User notifications linked to orders        |
| `Adresse`         | Delivery addresses per user                |

---

## Prerequisites

Before running the project, ensure you have:

- **Java 21** (JDK) installed
- **Maven 3.6+** installed
- **MySQL 8+** server running locally
- A MySQL database named `ecommerce` created

```sql
CREATE DATABASE ecommerce;
```

---

## Getting Started

### 1. Clone the Repository

```bash
git clone <repository-url>
cd ChriOnline
```

### 2. Set Up the Database

Run the SQL files **in this order**:

```bash
# 1. Create all tables
mysql -u root -p ecommerce < ecommerce.sql

# 2. Apply migrations
mysql -u root -p ecommerce < add_address_table.sql
mysql -u root -p ecommerce < add_numero_masque_column.sql
mysql -u root -p ecommerce < add_prix_unitaire_column.sql
mysql -u root -p ecommerce < add_uuid_commande_column.sql

# 3. (Optional) Load sample data
mysql -u root -p ecommerce < test_data.sql
```

### 3. Configure the Application

Edit `src/main/resources/application.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/ecommerce
db.username=root
db.password=YOUR_MYSQL_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

### 4. Build the Project

```bash
mvn clean install -DskipTests
```

---

## Running the Application

> ⚠️ **You must start the Server before the Client.**

### Start the Server

```bash
mvn exec:java -Dexec.mainClass="com.chrionline.chrionline.server.ServerApplication"
```

The server starts on **TCP port 5000** and waits for client connections.

### Start the Client

```bash
mvn clean javafx:run
```

Or using the Maven wrapper:

```bash
./mvnw clean javafx:run        # Linux / macOS
mvnw.cmd clean javafx:run      # Windows
```

The JavaFX window will open on the **Login screen**.

---

## Configuration

| Property          | Default Value                          | Description                  |
|-------------------|----------------------------------------|------------------------------|
| `db.url`          | `jdbc:mysql://localhost:3306/ecommerce`| MySQL JDBC URL               |
| `db.username`     | `root`                                 | MySQL username               |
| `db.password`     | *(empty)*                              | MySQL password               |
| `SERVER_HOST`     | `localhost`                            | Host the client connects to  |
| `SERVER_PORT`     | `5000`                                 | TCP port                     |
| `SOCKET_TIMEOUT`  | `30 000 ms`                            | Socket read timeout          |
| `MAX_THREADS`     | `50`                                   | Max concurrent client threads|
| `HASH_ALGORITHM`  | `SHA-256`                              | Password hashing algorithm   |

---

## Network Protocol

Client and server communicate using **JSON-serialized messages over TCP**.

### Request Format (`AppRequest`)

```json
{
  "controller": "Auth",
  "action": "login",
  "payload": "{\"email\":\"user@example.com\",\"password\":\"secret\"}",
  "authToken": "uuid-session-token"
}
```

### Response Format (`AppResponse`)

```json
{
  "status": "success",
  "message": "OK",
  "data": { "token": "...", "nom": "John", "role": "client" }
}
```

| Status    | Meaning                  |
|-----------|--------------------------|
| `success` | Request succeeded        |
| `error`   | Business logic error     |
| `bad_request` | Invalid input        |
| `unauthorized` | Auth required       |

---

## API Reference (Controllers)

### AuthController

| Action           | Auth Required | Description                     |
|------------------|---------------|---------------------------------|
| `login`          | No            | Authenticate user               |
| `register`       | No            | Create a new account            |
| `logout`         | Yes           | Invalidate session token        |
| `profil`         | Yes           | Get current user profile        |
| `updateprofil`   | Yes           | Update name/email               |
| `updatepassword` | Yes           | Change password                 |
| `listusers`      | Admin only    | List all users                  |
| `blockuser`      | Admin only    | Deactivate a user account       |
| `unblockuser`    | Admin only    | Reactivate a user account       |
| `deleteuser`     | Admin only    | Permanently delete a user       |

### ProduitController

| Action    | Auth Required | Description                  |
|-----------|---------------|------------------------------|
| `list`    | No            | Get all products             |
| `get`     | No            | Get a product by ID          |
| `add`     | Admin only    | Add a new product            |
| `update`  | Admin only    | Update product details       |
| `delete`  | Admin only    | Delete a product             |

### PanierController

| Action          | Auth Required | Description                 |
|-----------------|---------------|-----------------------------|
| `get`           | Yes           | Get current user's cart     |
| `addproduit`    | Yes           | Add a product to cart       |
| `removeproduit` | Yes           | Remove a product from cart  |
| `clear`         | Yes           | Empty the cart              |

### CommandeController

| Action       | Auth Required | Description              |
|--------------|---------------|--------------------------|
| `passer`     | Yes           | Place an order           |
| `historique` | Yes           | Get order history        |
| `get`        | Yes           | Get a specific order     |

### PaiementController

| Action       | Auth Required | Description              |
|--------------|---------------|--------------------------|
| `payer`      | Yes           | Process a payment        |
| `statut`     | Yes           | Get payment status       |

### AdresseController

| Action    | Auth Required | Description               |
|-----------|---------------|---------------------------|
| `list`    | Yes           | List user's addresses     |
| `add`     | Yes           | Add a new address         |
| `update`  | Yes           | Update an address         |
| `delete`  | Yes           | Remove an address         |

---

*ChriOnline v1.0.0 — Built with Java 21 + JavaFX 17 + MySQL*
