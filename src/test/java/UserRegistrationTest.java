package praktikum;

import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserRegistrationTest {

    private String token;

    @BeforeClass
    public static void setUp() {
        RestAssured.baseURI = "https://stellarburgers.nomoreparties.site";
    }

    @After
    public void tearDown() {
        if (token != null) {
            deleteUser(token);
            token = null;
        }
    }

    @Step("Удаление тестового пользователя")
    public void deleteUser(String token) {
        given()
                .header("Authorization", token)
                .when()
                .delete("/api/auth/user")
                .then()
                .statusCode(anyOf(is(200), is(202)))
                .body("success", equalTo(true));
    }

    @Step("Регистрация пользователя с email: {email}")
    private void registerUser(String email, String password, String name) {
        Map<String, String> user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);
        if (name != null) {
            user.put("name", name);
        }

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(403))); // 403, если уже существует
    }

    @Step("Логин пользователя и получение токена для email: {email}")
    private String loginUser(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Response loginResponse = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/login");

        return loginResponse.then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Test
    public void createUniqueUserReturnsSuccess() {
        String email = "unique_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";

        Map<String, String> user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);
        user.put("name", "Тест");

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(email.toLowerCase()));

        token = loginUser(email, password);
    }

    @Test
    public void createDuplicateUserReturnsForbidden() {
        String email = "duplicate_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";
        String name = "Тест";

        registerUser(email, password, name);

        Map<String, String> duplicateUser = new HashMap<>();
        duplicateUser.put("email", email);
        duplicateUser.put("password", password);
        duplicateUser.put("name", name);

        given()
                .contentType(ContentType.JSON)
                .body(duplicateUser)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(403)
                .body("message", equalTo("User already exists"));

        token = loginUser(email, password);
    }

    @Test
    public void createUserWithoutNameReturnsForbidden() {
        String email = "missing_name_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";

        Map<String, String> user = new HashMap<>();
        user.put("email", email);
        user.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(403)
                .body("message", equalTo("Email, password and name are required fields"));

    }
}