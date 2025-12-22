# E-Commerce Product Review & Rating System

## Project Overview
A microservices-based system where users can browse products, write reviews, rate products, and get personalized recommendations. This project covers all Spring Boot concepts while being beginner-friendly and practical.

---

## Technical Stack
- **Framework**: Spring Boot 3.x
- **Database**: MongoDB
- **Cache**: Redis
- **Message Queue**: Kafka
- **Security**: Spring Security with JWT
- **Testing**: JUnit 5, Mockito, TestContainers
- **Documentation**: SpringDoc OpenAPI
- **Build Tool**: Maven or Gradle

---

## Core Features & Requirements

### 1. User Management Service
- User registration and login
- Role-based access (USER, MODERATOR, ADMIN)
- JWT token generation and validation
- Password encryption using BCrypt
- Email verification (simulated via Kafka event)

### 2. Product Service
- CRUD operations for products (ADMIN only)
- Product search and filtering
- Product categories management
- View product details (Public access)

### 3. Review & Rating Service
- Users can write reviews for products
- 5-star rating system
- Edit/delete own reviews
- Moderators can flag/remove inappropriate reviews
- Calculate average ratings per product

### 4. Recommendation Service
- Generate product recommendations based on user ratings
- Cache recommendations in Redis
- Update recommendations asynchronously via Kafka

### 5. Notification Service
- Send notifications when reviews are moderated
- Email notifications for new followers/reviews
- Implemented using Kafka consumers

---

## Step-by-Step Implementation Path

### **PHASE 1: Project Setup & Foundation**

#### Step 1: Initialize Spring Boot Project
- Create a new Spring Boot project using Spring Initializr
- Add dependencies: Spring Web, Spring Data MongoDB, Spring Security, Lombok, Validation
- Set up your application.properties or application.yml file
- Configure MongoDB connection details
- Define your base package structure: controller, service, repository, model, dto, config, exception

#### Step 2: Setup MongoDB Collections
- Create User entity/document with fields: id, username, email, password, roles, createdAt, isEmailVerified
- Create Product entity: id, name, description, category, price, imageUrl, averageRating, totalReviews
- Create Review entity: id, productId, userId, rating (1-5), comment, createdAt, updatedAt, status (APPROVED/PENDING/REJECTED)
- Add proper MongoDB annotations (@Document, @Id, @Indexed, @DBRef where needed)

#### Step 3: Create DTOs and Mappers
- Create DTOs for requests: RegisterRequest, LoginRequest, ProductRequest, ReviewRequest
- Create DTOs for responses: UserResponse, ProductResponse, ReviewResponse, AuthResponse (with JWT token)
- Implement mapper classes or use MapStruct to convert between entities and DTOs

---

### **PHASE 2: Spring Security Implementation**

#### Step 4: Configure Spring Security
- Create SecurityConfig class extending WebSecurityConfigurerAdapter or using SecurityFilterChain
- Disable CSRF for REST API
- Configure stateless session management
- Define public endpoints (login, register, product view)
- Define protected endpoints with role-based access

#### Step 5: Implement JWT Authentication
- Create JwtUtil class for generating and validating JWT tokens
- Implement UserDetailsService to load user from MongoDB
- Create JwtAuthenticationFilter to intercept requests and validate tokens
- Add filter to Spring Security filter chain

#### Step 6: Create Authentication Endpoints
- POST /api/auth/register - Create new user account
- POST /api/auth/login - Authenticate and return JWT token
- GET /api/auth/me - Get current authenticated user details
- Implement password encoding in registration
- Add validation annotations on DTOs

---

### **PHASE 3: Core Business Logic**

#### Step 7: Implement Product Service
- Create ProductRepository extending MongoRepository
- Implement ProductService with methods: createProduct, updateProduct, deleteProduct, getProductById, getAllProducts, searchProducts
- Create ProductController with REST endpoints
- Add pagination support using Pageable
- Implement search by name, category, price range

#### Step 8: Implement Review Service
- Create ReviewRepository with custom queries
- Implement ReviewService with methods: createReview, updateReview, deleteReview, getReviewsByProduct, getReviewsByUser
- Add business logic: users can't review same product twice, users can only edit their own reviews
- Update product's average rating when review is added/updated/deleted
- Create ReviewController with appropriate endpoints

#### Step 9: Implement Authorization Rules
- Use @PreAuthorize annotations on controller methods
- ADMIN can create/update/delete products
- USER can create/update/delete their own reviews
- MODERATOR can approve/reject any review
- Public can view products and approved reviews

---

### **PHASE 4: Redis Caching**

#### Step 10: Setup Redis
- Add Spring Data Redis dependency
- Configure Redis connection in application properties
- Create RedisConfig class with RedisTemplate and CacheManager beans
- Enable caching using @EnableCaching

#### Step 11: Implement Caching Strategy
- Cache product details using @Cacheable on getProductById method
- Cache product list using cache key with pagination parameters
- Evict cache using @CacheEvict when product is updated/deleted
- Cache user recommendations with TTL of 1 hour
- Cache product average ratings to reduce database queries

---

### **PHASE 5: Kafka Integration**

#### Step 12: Setup Kafka
- Add Spring Kafka dependency
- Configure Kafka bootstrap servers in application properties
- Create KafkaConfig class with producer and consumer configurations
- Define topic names as constants

#### Step 13: Implement Kafka Producers
- Create KafkaProducerService to send messages
- Send event when user registers (for email verification)
- Send event when review is created (for notification)
- Send event when review is moderated (to notify user)
- Send event when product rating changes (to update recommendations)

#### Step 14: Implement Kafka Consumers
- Create KafkaConsumerService with @KafkaListener methods
- Consumer 1: Process email verification events
- Consumer 2: Process review notification events
- Consumer 3: Process rating update events and regenerate recommendations
- Add proper error handling and logging

---

### **PHASE 6: Advanced Features**

#### Step 15: Implement Recommendation Service
- Create RecommendationService class
- Algorithm: Recommend products from categories user has highly rated (4-5 stars)
- Store recommendations in Redis with user ID as key
- Refresh recommendations when Kafka event is received
- Create endpoint GET /api/recommendations to fetch cached recommendations

#### Step 16: Add Global Exception Handling
- Create GlobalExceptionHandler with @ControllerAdvice
- Handle specific exceptions: ResourceNotFoundException, UnauthorizedException, ValidationException
- Handle JWT exceptions: ExpiredJwtException, SignatureException
- Return proper HTTP status codes and error response format
- Add custom exception classes

#### Step 17: Add Request Validation
- Use @Valid annotation on controller method parameters
- Add validation annotations on DTOs: @NotBlank, @Email, @Size, @Min, @Max
- Create custom validators if needed (e.g., validate rating is between 1-5)
- Handle validation errors in GlobalExceptionHandler

---

### **PHASE 7: Testing**

#### Step 18: Unit Testing
- Add JUnit 5 and Mockito dependencies
- Test service layer methods by mocking repositories
- Test JWT utility methods
- Test mapper classes
- Test validation logic
- Aim for 70%+ code coverage

#### Step 19: Integration Testing
- Add TestContainers dependency for MongoDB and Redis
- Write integration tests for repositories
- Test REST endpoints using MockMvc or RestTemplate
- Test security configurations (authenticated vs unauthenticated requests)
- Test Kafka producer and consumer integration

#### Step 20: Security Testing
- Test authentication flows (successful login, failed login)
- Test authorization (access denied for unauthorized roles)
- Test JWT token expiration
- Test password encryption
- Test SQL injection prevention (MongoDB injection)

---

### **PHASE 8: Documentation & Deployment**

#### Step 21: API Documentation
- Add SpringDoc OpenAPI dependency
- Configure Swagger UI
- Add @Operation, @ApiResponse annotations on controller methods
- Document security schemes (JWT Bearer token)
- Group endpoints by tags
- Access Swagger UI at /swagger-ui.html

#### Step 22: Dockerization
- Create Dockerfile for your Spring Boot application
- Use multi-stage build (build stage + runtime stage)
- Create docker-compose.yml file
- Include services: app, mongodb, redis, kafka, zookeeper
- Configure environment variables
- Map ports appropriately

#### Step 23: Application Properties Organization
- Create multiple property files: application-dev.yml, application-prod.yml
- Use environment-specific configurations
- Externalize sensitive data (use environment variables)
- Configure different MongoDB, Redis, Kafka URLs for each environment
- Set up proper logging levels

#### Step 24: Deployment Preparation
- Create application startup scripts
- Configure health check endpoint using Spring Actuator
- Add monitoring endpoints: /actuator/health, /actuator/metrics
- Set up proper logging (use SLF4J with Logback)
- Create README.md with setup instructions

---

## Learning Outcomes

By completing this project, you'll learn:
- ✅ Spring Boot fundamentals and project structure
- ✅ MongoDB integration and document modeling
- ✅ Spring Security with JWT authentication
- ✅ Role-based authorization
- ✅ Redis caching strategies
- ✅ Kafka event-driven architecture
- ✅ RESTful API design
- ✅ Exception handling and validation
- ✅ Unit and integration testing
- ✅ Docker containerization
- ✅ API documentation with Swagger
- ✅ Deployment concepts

---

## Optional Enhancements (After Completing Main Project)

1. **Add API Rate Limiting** using Redis
2. **Implement Refresh Tokens** for better security
3. **Add File Upload** for product images
4. **Implement Full-Text Search** using MongoDB text indexes
5. **Add Metrics and Monitoring** using Micrometer and Prometheus
6. **Deploy to Cloud** (AWS, Azure, or Heroku)

---

Start with Phase 1 and progress sequentially. Each phase builds upon the previous one. Take your time to understand each concept before moving forward. Good luck! 🚀