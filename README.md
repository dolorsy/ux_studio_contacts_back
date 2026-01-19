# Contacts API

## How to Run

### Using Docker Compose
```bash
docker-compose up -d
```

Application will be available at `http://localhost:8080`


## Project Structure

```
src/main/kotlin/org/uxstudio/contacts/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── exception/       # Exception handlers
├── model/           # Entity models
├── repository/      # Data repositories
└── service/         # Business logic
```

## API Endpoints

- `POST /api/contacts` - Create contact
- `GET /api/contacts` - List all contacts
- `GET /api/contacts/{id}` - Get contact by ID
- `PUT /api/contacts/{id}` - Update contact
- `DELETE /api/contacts/{id}` - Delete contact
