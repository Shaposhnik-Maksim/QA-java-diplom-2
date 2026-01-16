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

public class UserLoginTest {

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
        user.put("name", name);

        given()
                .contentType(ContentType.JSON)
                .body(user)
                .when()
                .post("/api/auth/register")
                .then()
                .statusCode(anyOf(is(200), is(403)));
    }

    @Step("Логин пользователя и получение accessToken")
    private String loginUser(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        Response response = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/login");

        response.then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("accessToken", notNullValue())
                .body("user.email", equalTo(email.toLowerCase()));

        return response.then().extract().path("accessToken");
    }

    @Step("Попытка логина с неверными данными (email: {email})")
    private void loginWithInvalidCredentials(String email, String password) {
        Map<String, String> credentials = new HashMap<>();
        credentials.put("email", email);
        credentials.put("password", password);

        given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/login")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("email or password are incorrect"));
    }

    @Test
    public void loginWithValidCredentialsReturnsSuccess() {
        String email = "login_test_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";

        registerUser(email, password, "Логин Тест");

        token = loginUser(email, password);
    }

    @Test
    public void loginWithInvalidCredentialsReturnsUnauthorized() {
        String email = "wrong_" + System.currentTimeMillis() + "@test.ru";
        String password = "wrongPassword";

        loginWithInvalidCredentials(email, password);
    }
}