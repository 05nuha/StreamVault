# StreamVault — Code Explained Line by Line

---

## pom.xml

This is not Java code — it is an XML config file Maven reads before doing anything.

```xml
<groupId>com.streamvault</groupId>
<artifactId>streamvault-phase4</artifactId>
<packaging>war</packaging>
```
- groupId = your organisation name (like a package namespace)
- artifactId = the project name
- packaging = war means "build this as a web app file", not a regular jar

```xml
<maven.compiler.source>11</maven.compiler.source>
<maven.compiler.target>11</maven.compiler.target>
```
Tells Maven to compile the code using Java 11 syntax rules.

```xml
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.33</version>
</dependency>
```
This tells Maven "go download the MySQL JDBC driver from the internet".
Without this, Java has no idea how to talk to MySQL.
The driver is the translator between Java and MySQL's communication protocol.

```xml
<dependency>
    <groupId>org.mindrot</groupId>
    <artifactId>jbcrypt</artifactId>
    <version>0.4</version>
</dependency>
```
Downloads the BCrypt library so we can use BCrypt.hashpw() and BCrypt.checkpw()
in our Java code.

```xml
<dependency>
    <groupId>jakarta.servlet</groupId>
    <artifactId>jakarta.servlet-api</artifactId>
    <scope>provided</scope>
</dependency>
```
Downloads the Servlet API classes (HttpServlet, HttpServletRequest, etc.)
scope=provided means "Tomcat already has this, don't include it in the WAR"

---

## DatabaseConnection.java

```java
package com.streamvault.db;
```
Every Java file must declare which package it belongs to.
This file lives in the db/ folder so its package is com.streamvault.db.

```java
private static final String URL = "jdbc:mysql://localhost:3306/streamvault";
private static final String USER = "root";
private static final String PASS = "yourpassword";
```
- static = belongs to the class, not an instance (only one copy exists)
- final = cannot be changed after it is set (like a constant)
- The URL tells JDBC: use MySQL protocol, connect to localhost on port 3306,
  use the database named streamvault

```java
public static Connection getConnection() throws SQLException {
    return DriverManager.getConnection(URL, USER, PASS);
}
```
DriverManager is a built-in Java class that knows how to open database connections.
It reads the URL and figures out which driver to use (MySQL in this case).
throws SQLException means "this method might fail — the caller must handle that".
We made it static so any class can call DatabaseConnection.getConnection()
without creating a DatabaseConnection object first.

---

## AuthService.java

### The login method

```java
public static boolean login(String email, String pw) {
    String sql = "SELECT password FROM Users WHERE email = ?";
```
We only select the password column — we don't need anything else for login.
The ? is a placeholder — this is called a PreparedStatement.
We never put variables directly in the SQL string because that would allow
SQL injection attacks (someone could type ' OR '1'='1 and bypass login).

```java
try (Connection c = DatabaseConnection.getConnection();
     PreparedStatement ps = c.prepareStatement(sql)) {
```
try-with-resources: Java automatically closes c and ps when the block ends,
even if an error occurs. This prevents memory leaks (unclosed connections).

```java
    ps.setString(1, email);
```
Replaces the ? at position 1 with the email value safely.
JDBC escapes any special characters so SQL injection is impossible.

```java
    ResultSet rs = ps.executeQuery();
    if (rs.next()) {
        return BCrypt.checkpw(pw, rs.getString(1));
    }
```
executeQuery() runs the SELECT and returns a ResultSet (like a table of results).
rs.next() moves to the first row — returns false if no rows found (email not registered).
rs.getString(1) gets the first column of that row (the stored hash).
BCrypt.checkpw() hashes the entered password the same way and compares —
it never "decrypts" the stored hash, it re-hashes and compares.

```java
} catch (SQLException e) {
    e.printStackTrace();
}
return false;
```
If anything goes wrong (DB down, bad query, etc.) we catch the error,
print it for debugging, and return false (treat as failed login).

---

### The register method

```java
String hash = BCrypt.hashpw(pw, BCrypt.gensalt(12));
```
gensalt(12) generates a random "salt" with cost factor 12.
A salt is random data added to the password before hashing so two users with
the same password end up with completely different hashes.
The 12 means BCrypt runs 2^12 = 4096 rounds of hashing — slow enough to
make brute-force attacks impractical.

```java
String sql = "INSERT INTO Users(full_name, email, password, country) VALUES(?, ?, ?, ?)";
```
Four ? placeholders for four values. We never concatenate strings into SQL.

```java
ps.setString(1, name);
ps.setString(2, email);
ps.setString(3, hash);   // storing the HASH not the real password
ps.setString(4, country);
ps.executeUpdate();
```
executeUpdate() is used for INSERT/UPDATE/DELETE — not executeQuery()
which is only for SELECT statements.

---

## LoginServlet.java

```java
@WebServlet("/login")
```
This annotation tells Tomcat: "when a request comes in for /login, use this class".
No need to configure anything in web.xml — the annotation does it automatically.

```java
public class LoginServlet extends HttpServlet {
```
extends HttpServlet means this class inherits all the web server behaviour
from the HttpServlet base class. We just override the specific methods we need.

```java
protected void doGet(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
    req.getRequestDispatcher("/login.html").forward(req, res);
}
```
doGet handles GET requests (someone types the URL in a browser).
getRequestDispatcher + forward = sends the request to a different resource
(login.html) without the browser knowing. The URL stays as /login.

```java
protected void doPost(HttpServletRequest req, HttpServletResponse res) {
    String email = req.getParameter("email");
    String password = req.getParameter("password");
```
doPost handles form submissions (method="POST" in the HTML form).
getParameter("email") reads the value of the input with name="email" from the form.

```java
    if (AuthService.login(email, password)) {
        HttpSession session = req.getSession();
        session.setAttribute("user", email);
        res.sendRedirect("home");
```
req.getSession() creates a session if one doesn't exist, or returns the existing one.
setAttribute stores the email under the key "user" — any servlet can now read
session.getAttribute("user") to know who is logged in.
sendRedirect tells the browser to go to a new URL — the browser makes a new request.
(Unlike forward which is server-side only)

```java
    } else {
        res.sendRedirect("login.html?error=1");
    }
```
?error=1 is a query parameter. The JavaScript in login.html reads this and
shows the error message. We can't show an error by just reloading the form
because POST data doesn't survive a redirect — so we pass the signal in the URL.

---

## RegisterServlet.java

```java
AuthService.register(name, email, password, country);
res.sendRedirect("login.html?registered=1");
```
After registering, we redirect to the login page with ?registered=1 in the URL.
login.html's JavaScript reads this and shows the green success message.
We redirect instead of forwarding because if the user refreshes the page after
a forward it would submit the form again and try to register twice.
This is called the POST-Redirect-GET pattern.

---

## LogoutServlet.java

```java
HttpSession session = req.getSession(false);
```
getSession(false) means "get the session IF it exists, but don't create a new one".
getSession() or getSession(true) would create a new empty session if none exists —
we don't want that during logout.

```java
if (session != null) {
    session.invalidate();
}
```
invalidate() destroys the session on the server and tells the browser to delete
the session cookie. The user is now completely logged out.

---

## login.html

```html
<form action="login" method="POST">
```
action="login" = send this form to the /login URL
method="POST" = use HTTP POST (form data goes in the request body, not the URL)
If we used GET the password would appear in the URL bar — never do that.

```html
<input type="password" name="password" minlength="8" required>
```
type="password" = browser hides the characters while typing
minlength="8" = browser blocks submission if less than 8 characters
required = browser blocks submission if field is empty
These are client-side validations — convenient but not security.
The real security is BCrypt on the server side.

```javascript
const params = new URLSearchParams(window.location.search);
if (params.get('error') == '1') {
    document.getElementById('error-msg').style.display = 'block';
}
```
window.location.search gives the query string (?error=1 or ?registered=1).
URLSearchParams parses it into key-value pairs.
We then show or hide the relevant message div based on what's in the URL.

---

## web.xml

```xml
<welcome-file-list>
    <welcome-file>login.html</welcome-file>
</welcome-file-list>
```
When someone visits localhost:8080/streamvault/ with no specific page,
Tomcat serves login.html automatically.
Without this Tomcat would show a directory listing or 404 error.

---

## setup.sql

```sql
CREATE DATABASE IF NOT EXISTS streamvault;
```
IF NOT EXISTS means it won't crash if the database already exists.

```sql
FOREIGN KEY (user_id) REFERENCES Users(user_id)
```
This enforces referential integrity — you cannot add a Subscription row
with a user_id that doesn't exist in the Users table.
MySQL rejects it automatically.

```sql
CREATE FULLTEXT INDEX ft_content_title ON Content_Items(title);
```
A regular index does exact or range matches.
A FULLTEXT index supports keyword searching inside text — needed for
search features like "find all content containing 'dark'".

---

## How it all connects

```
pom.xml
  └── tells Maven to download mysql-connector, jbcrypt, servlet-api

DatabaseConnection.java
  └── uses mysql-connector to open a Connection to MySQL

AuthService.java
  └── uses DatabaseConnection to get a Connection
  └── uses jbcrypt to hash/verify passwords
  └── runs SQL through PreparedStatements

LoginServlet.java
  └── uses jakarta.servlet (HttpServlet, HttpServletRequest, HttpServletResponse)
  └── calls AuthService.login()
  └── manages sessions

RegisterServlet.java
  └── calls AuthService.register()

LogoutServlet.java
  └── destroys the session

login.html / register.html
  └── HTML forms that send POST requests to the servlets
  └── JavaScript reads URL params to show messages

web.xml
  └── Tomcat reads this to configure the app

setup.sql
  └── creates the MySQL tables the servlets query against
```
