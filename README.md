# 📊 Trading Dashboard (Spring Boot)

A real-time trading simulation platform built with Spring Boot.  
It supports order placement, order matching, portfolio tracking, and live market updates via WebSockets.

---

##  Features

###  Trading System
- Place buy/sell orders
- Limit & market order support
- Automatic order matching engine
- Trade execution simulation

###  Portfolio Management
- Track positions per stock
- Real-time portfolio valuation
- Portfolio updates after trades

###  Real-Time Market Data
- WebSocket-based live updates
- Order book streaming
- Price feed simulation
- Portfolio updates pushed instantly

###  Core Engine
- Matching engine (`OrderMatchingService`)
- Simulated market price generator (`PriceFeedSimulator`)
- Event-driven architecture

---

##  Tech Stack

- Java 17+
- Spring Boot
- Spring Web
- Spring WebSocket
- Spring Data JPA
- Maven
- H2 / SQL (via `data.sql`)
- Vanilla HTML (basic frontend)

---

##  Project Structure

│
├── controller # REST & WebSocket controllers
├── service # Business logic (matching, portfolio, market data)
├── model # Entities (Order, Trade, Portfolio, etc.)
├── dto # API request/response models
├── repository # JPA repositories
├── websocket # Real-time data publishers
├── events # Domain events
├── config # WebSocket configuration



---

##  How to Run

### 1. Clone repository
```bash
git clone https://github.com/your-username/trading-dashboard.git
cd trading-dashboard
Run with:
mvn spring-boot:run

Open in browser:
http://localhost:8080

📡 API Overview
Orders
POST /orders – place order
GET /orders – list orders
Portfolio
GET /portfolio/{id} – get portfolio snapshot
Market Data
WebSocket endpoint for live updates (configured in WebSocketConfig)


🔄 Architecture Overview
REST APIs handle order placement and queries
Matching engine processes orders in backend
WebSocket pushes real-time updates:
Price changes
Order book updates
Portfolio changes
Simulated market feed generates price movement

📌 Notes

This project is a simulation-based trading system, not connected to real financial markets.

It is designed for:

Learning Spring Boot architecture
Understanding trading systems
Practicing real-time WebSocket systems
Backend portfolio projects
