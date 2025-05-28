TinyURL - URL Shortening Service
TinyURL is a full-stack web application that allows users to shorten long URLs, track clicks, and manage user-specific short URLs. Built as a course project, it demonstrates a modern microservices architecture with a Spring Boot backend, React frontend, and multiple databases (MongoDB, Redis, Cassandra). The application is deployed on Render and accessible at https://shorturl.runmydocker-app.com/.
Quick Links

Live Demo: https://shorturl.runmydocker-app.com/
API Documentation (Swagger): https://shorturl.runmydocker-app.com/swagger-ui.html
Backend Repository: https://github.com/elad9219/tinyurl
Frontend Repository: https://github.com/elad9219/tinyurl-frontend

Table of Contents

Features
Technologies
Installation
Usage
Screenshots
Project Structure
Contributing
License
Contact

Features

Create New User: Register a new user to manage short URLs.
Create Tiny URL: Shorten long URLs into compact links (e.g., https://shorturl.runmydocker-app.com/OXeqgq/).
User Information: View user details, including total clicks and short URLs with monthly click counts.
Click Details: Track click history for each short URL, including timestamps and original URLs.
URL Normalization: Automatically formats URLs with https:// and optional www. for consistency.
Responsive Design: User-friendly React interface for desktop and mobile.
Database Integration: Uses MongoDB for user data, Redis for URL mappings, and Cassandra for click tracking.
Dockerized Deployment: Packaged as a Docker image (elad9219/tinyurl:005) and deployed on Render.
CORS Support: Securely handles cross-origin requests for the frontend.

Technologies

Backend: Spring Boot, Java 11, Maven
Frontend: React, TypeScript, Node.js 14+, npm
Databases:
MongoDB: User data and short URL metadata
Redis: Fast key-value storage for URL mappings
Cassandra: Click tracking for analytics


Containerization: Docker
Deployment: Render
Version Control: Git, GitHub
Other: SLF4J (logging), Jackson (JSON processing)

Installation
Follow these steps to set up the project locally.
Prerequisites

Java 11
Node.js 14+
Docker
MongoDB, Redis, and Cassandra instances (or use provided remote instances)
Git

Backend Setup

Clone the backend repository:git clone https://github.com/elad9219/tinyurl.git
cd tinyurl


Configure environment variables in src/main/resources/application.properties:spring.data.mongodb.uri=mongodb://admin:admin@node128.codingbc.com:27000/admin?authSource=admin
spring.redis.host=node128.codingbc.com
spring.redis.port=6380
spring.redis.password=admin
cassandra.contact-points=node128.codingbc.com
cassandra.port=9043
cassandra.username=admin
cassandra.password=admin
base.url=https://shorturl.runmydocker-app.com/


Build the project:mvn clean install


Run the backend:mvn spring-boot:run



Frontend Setup

Clone the frontend repository:git clone https://github.com/elad9219/tinyurl-frontend.git
cd tinyurl-frontend


Install dependencies:npm install


Build the frontend:npm run build


Copy the build to the backend's static resources:cp -r build/* /path/to/tinyurl/src/main/resources/static/



Docker Setup

Build the Docker image:cd /path/to/tinyurl
docker build --platform linux/amd64 -t elad9219/tinyurl:005 .


Run the container:docker run --platform linux/amd64 -p 8080:8080 \
  -e SPRING_DATA_MONGODB_URI=mongodb://admin:admin@node128.codingbc.com:27000/admin?authSource=admin \
  -e SPRING_REDIS_HOST=node128.codingbc.com \
  -e SPRING_REDIS_PORT=6380 \
  -e SPRING_REDIS_PASSWORD=admin \
  -e CASSANDRA_CONTACT_POINTS=node128.codingbc.com \
  -e CASSANDRA_PORT=9043 \
  -e CASSANDRA_USERNAME=admin \
  -e CASSANDRA_PASSWORD=admin \
  elad9219/tinyurl:005


Access the app at http://localhost:8080.

Usage

Create a User:
Enter a username (e.g., dsfdsf) and click "Create User".
Success message: "User created successfully".


Create a Tiny URL:
Enter a username and a long URL (e.g., https://www.one.co.il).
Receive a short URL (e.g., https://shorturl.runmydocker-app.com/OXeqgq/).


View User Information:
Enter a username to see total clicks and short URLs with click counts.


View Click Details:
Enter a username to see a list of clicks with timestamps and original URLs.


Click a Short URL:
Visit the short URL (e.g., https://shorturl.runmydocker-app.com/OXeqgq/) to redirect to the original URL.



Example
Create User: dsfdsf
Create Tiny URL: dsfdsf, https://www.one.co.il
Result: https://shorturl.runmydocker-app.com/OXeqgq/
User Info: Name: dsfdsf, Total Clicks: 2, Short URLs: OXeqgq (1 click), HLwF1E (1 click)
Click Details: 27/05/2025 17:34:56 - https://www.ynet.co.il/ (OXeqgq)

Screenshots
Add screenshots to showcase the app's interface. Place images in a screenshots/ folder and update the links below:

Homepage: 
Create User: 
Create Tiny URL: 
User Information: 
Click Details: 

Instructions:

Take screenshots of the app (e.g., homepage, create user form, tiny URL result, user info, click details).
Create a screenshots/ folder in the repository:mkdir screenshots


Save images as homepage.png, create-user.png, etc.
Commit and push:git add screenshots/
git commit -m "Add screenshots for README"
git push origin main



Project Structure
Backend (elad9219/tinyurl)
tinyurl/
├── src/
│   ├── main/
│   │   ├── java/com/handson/tinyurl/
│   │   │   ├── config/CorsConfig.java
│   │   │   ├── controller/AppController.java
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── service/
│   │   │   ├── util/
│   │   ├── resources/
│   │   │   ├── static/ (React build files)
│   │   │   ├── application.properties
├── pom.xml
├── Dockerfile

Frontend (elad9219/tinyurl-frontend)
tinyurl-frontend/
├── src/
│   ├── components/
│   ├── utils/
│   │   ├── globals.ts
│   ├── App.tsx
├── public/
├── package.json
├── tsconfig.json

Contributing
Contributions are welcome! To contribute:

Fork the repository.
Create a feature branch: git checkout -b feature-name.
Commit changes: git commit -m 'Add feature'.
Push to the branch: git push origin feature-name.
Open a pull request.

License
This project is licensed under the MIT License - see the LICENSE file for details.
Contact

Author: Elad Tennenboim
GitHub: elad9219
Email: Replace with your email
LinkedIn: Add your LinkedIn profile (optional)


Thank you for exploring TinyURL! Feel free to reach out with questions or feedback.
