package auth

class EmailAlreadyExistsException(email: String) :
    RuntimeException("User with email '$email' already exists")
