# 📋 Task Manager — Backend API

A production-ready REST API for task management built with **Spring Boot 3.5** and **Java 21**.

**Live URL:** [https://tms-backend-trnk.onrender.com](https://tms-backend-trnk.onrender.com)

---

## 🛠 Tech Stack

| Technology | Purpose |
|---|---|
| **Spring Boot 3.5** | Web framework |
| **Spring Security** | Authentication & authorization |
| **JWT (jjwt 0.12)** | Token-based authentication |
| **Spring Data JPA** | Database ORM |
| **MySQL** | Production database (Railway) |
| **Lombok** | Boilerplate reduction |
| **Maven** | Build tool |
| **Docker** | Containerized deployment |

---

## 📁 Project Structure

```
backend/
├── src/main/java/com/taskmanager/
│   ├── config/          # Security, CORS configuration
│   ├── controller/      # REST controllers (Auth, Task)
│   ├── dto/             # Data Transfer Objects
│   ├── entity/          # JPA entities (User, Task)
│   ├── exception/       # Global exception handling
│   ├── mapper/          # Entity ↔ DTO mappers
│   ├── repository/      # JPA repositories & specifications
│   ├── security/        # JWT filter & service
│   └── service/         # Business logic
├── src/main/resources/
│   └── application.yml  # App configuration
├── Dockerfile           # Docker build config
├── .env                 # Environment variables (local)
└── pom.xml              # Maven dependencies
```

---

## 🔌 API Endpoints

### Authentication (Public)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `POST` | `/api/auth/register` | Register a new user |
| `POST` | `/api/auth/login` | Login & get JWT token |

**Register Request:**
```json
{
    "fullName": "John Doe",
    "email": "john@gmail.com",
    "password": "password123"
}
```

**Login Request:**
```json
{
    "email": "john@gmail.com",
    "password": "password123"
}
```

### Tasks (Requires JWT)

> Add header: `Authorization: Bearer <token>`

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/tasks` | Get all tasks (paginated) |
| `GET` | `/api/tasks/{id}` | Get task by ID |
| `POST` | `/api/tasks` | Create a new task |
| `PUT` | `/api/tasks/{id}` | Update a task |
| `PATCH` | `/api/tasks/{id}/status` | Update task status |
| `DELETE` | `/api/tasks/{id}` | Delete a task |

**Query Parameters** for `GET /api/tasks`:
- `page` — Page number (default: 0)
- `size` — Items per page (default: 10)
- `status` — Filter by `TODO`, `IN_PROGRESS`, `DONE`
- `search` — Search by title

---

## 🚀 Getting Started

### Prerequisites
- Java 21
- Maven
- MySQL

### Local Setup

1. **Clone the repo:**
   ```bash
   git clone <your-repo-url>
   cd backend
   ```

2. **Create a `.env` file:**
   ```env
   DB_URL=jdbc:mysql://localhost:3306/taskmanager
   DB_USERNAME=root
   DB_PASSWORD=your_password
   PORT=8080
   ```

3. **Run the application:**
   ```bash
   ./mvnw spring-boot:run
   ```

The API will be available at `http://localhost:8080`

### Docker

```bash
docker build -t tms-backend .
docker run -p 8080:8080 --env-file .env tms-backend
```

---

## 🌐 Deployment (Render)

1. Push code to GitHub
2. Create a **Web Service** on [Render](https://render.com)
3. Set **Root Directory** to `backend`
4. Render auto-detects the `Dockerfile`
5. Add environment variables: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `PORT`

---

## 📄 License

This project is for educational purposes.
