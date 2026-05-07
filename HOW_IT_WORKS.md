# StreamVault Phase 4 — How Everything Works

---

## THE BIG PICTURE

When you type `localhost:8080/streamvault/login.html` in your browser, here is
exactly what happens behind the scenes:

```
Browser → Tomcat (web server) → Your Java Servlet → MySQL Database → back to Browser
```

Think of it like a restaurant:
- Browser = the customer placing an order
- Tomcat = the waiter who takes the order to the kitchen
- Servlet = the chef who processes it
- MySQL = the fridge/storage where all the data lives

---

## THE TOOLS

### 1. Maven
Maven is a build tool. It does two things:
- Downloads all the libraries your project needs (MySQL driver, BCrypt, Servlet API)
  from the internet automatically — you just list them in pom.xml
- Packages all your Java files + HTML pages into one deployable file called a WAR file

Without Maven you would have to manually download every library and compile every
Java file yourself.

**The command we ran:** `mvn package`
This compiled all your Java code and zipped everything into:
`target/streamvault-phase4-1.0-SNAPSHOT.war`

---

### 2. Tomcat
Tomcat is a web server that understands Java web apps (servlets).
It sits on your laptop and listens on port 8080. When a browser sends a request to
`localhost:8080`, Tomcat receives it and figures out which servlet should handle it.

**How we started it:**
We copied the WAR file into Tomcat's `webapps/` folder and ran `startup.bat`.
Tomcat automatically unpacked the WAR and made the app available at
`localhost:8080/streamvault/`

---

### 3. JDBC (Java Database Connectivity)
JDBC is Java's built-in way of talking to a database.
It lets your Java code send SQL queries to MySQL and get results back.

`DatabaseConnection.java` is the bridge — it holds the URL, username, and password
of your MySQL database and gives a Connection object to whoever needs it.

Every time a servlet needs the database it calls:
```java
Connection c = DatabaseConnection.getConnection();
```

---

### 4. BCrypt
BCrypt is a password hashing library. Instead of storing passwords as plain text
(which is dangerous), it scrambles them into a long unreadable string like:
`$2a$12$eImiTXuWVxfM37uY4JANjQ...`

Even if someone hacks the database, they cannot reverse this back to the original password.

When a user registers:  `BCrypt.hashpw(password, BCrypt.gensalt(12))` → stores the hash
When a user logs in:    `BCrypt.checkpw(enteredPassword, storedHash)` → returns true/false

The number 12 is the "cost factor" — how many rounds of scrambling to do.
Higher = more secure but slower.

---

### 5. pom.xml
This is Maven's configuration file. It lists:
- What your project is called
- What Java version to use
- What libraries (dependencies) to download

The three libraries we added:
```
mysql-connector-java  → lets Java talk to MySQL
jbcrypt               → the BCrypt password hashing library
jakarta.servlet-api   → the Servlet classes (HttpServlet, HttpServletRequest, etc.)
```

---

## THE FILES — WHAT EACH ONE DOES

### DatabaseConnection.java
Holds the MySQL connection details (URL, username, password).
Has one method: `getConnection()` which opens and returns a database connection.
Every servlet uses this to get a connection.

---

### AuthService.java
The business logic layer — handles login and registration.

`login(email, password)`:
1. Queries the Users table for that email
2. Uses BCrypt to compare the entered password to the stored hash
3. Returns true if they match, false if not

`register(name, email, password, country)`:
1. Hashes the password with BCrypt (12 rounds)
2. Inserts a new row into the Users table

---

### LoginServlet.java — mapped to `/login`
Handles the login form.

GET request (someone types /login in the browser):
→ Shows login.html

POST request (someone submits the login form):
→ Gets email + password from the form
→ Calls AuthService.login()
→ If true: saves email in session, redirects to /home
→ If false: redirects back to login.html?error=1

---

### RegisterServlet.java — mapped to `/register`
Handles the registration form.

POST request (someone submits the register form):
→ Gets name, email, password, country from the form
→ Calls AuthService.register()
→ Redirects to login.html?registered=1

---

### LogoutServlet.java — mapped to `/logout`
Simple — destroys the session and sends the user back to login.html.
`session.invalidate()` is what kills the session.

---

### login.html / register.html
Plain HTML forms. The `action="login"` on the form tag is what tells the browser
to send the form data to the LoginServlet when submitted.
The JavaScript at the bottom reads the URL (e.g. ?error=1) and shows/hides
the error or success messages accordingly.

---

### web.xml
Tomcat reads this file when loading the app. It tells Tomcat:
- The app's display name
- What the welcome/home page is (login.html)

---

## THE FULL FLOW — REGISTRATION

```
1. User opens: localhost:8080/streamvault/register.html
2. Fills in the form and clicks Register
3. Browser sends POST request to /register with the form data
4. Tomcat receives it and passes it to RegisterServlet
5. RegisterServlet calls AuthService.register(name, email, password, country)
6. AuthService hashes the password with BCrypt
7. AuthService runs: INSERT INTO Users VALUES (name, email, hashedPassword, country)
8. JDBC sends that SQL to MySQL
9. MySQL stores the new row
10. RegisterServlet redirects browser to login.html?registered=1
11. login.html JavaScript sees ?registered=1 and shows green "Account created!" message
```

---

## THE FULL FLOW — LOGIN

```
1. User fills in email + password on login.html and clicks Login
2. Browser sends POST request to /login
3. Tomcat passes it to LoginServlet
4. LoginServlet calls AuthService.login(email, password)
5. AuthService runs: SELECT password FROM Users WHERE email = ?
6. JDBC sends that SQL to MySQL, gets back the stored hash
7. BCrypt.checkpw(enteredPassword, storedHash) → true or false
8. If true:  session.setAttribute("user", email) → redirect to /home
9. If false: redirect to login.html?error=1
```

---

## HOW THE SESSION WORKS

A session is like a temporary ID card Tomcat gives you after login.
When LoginServlet does `session.setAttribute("user", email)`, Tomcat:
- Creates a session object on the server
- Gives the browser a cookie with a session ID
- Every future request from that browser sends the cookie automatically
- Kety's servlets (Dashboard, etc.) read the session with `session.getAttribute("user")`
  to know who is logged in

When LogoutServlet calls `session.invalidate()`, that ID card is destroyed.

---

## WHY THIS STRUCTURE?

```
db/        → DatabaseConnection only. One place that knows the DB credentials.
services/  → AuthService only. Business logic, no HTTP stuff.
servlets/  → LoginServlet, RegisterServlet, LogoutServlet. HTTP handling only.
webapp/    → HTML, CSS, JS. Everything the browser directly downloads.
```

Keeping these separate means if you change the database you only touch db/.
If you change the login logic you only touch services/.
If you change the page design you only touch webapp/.
