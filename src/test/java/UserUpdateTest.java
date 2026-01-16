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

public class UserUpdateTest {

    private String token; // глобальный токен пользователя

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

    @Step("Регистрация пользователя и получение токена для email: {email}")
    private String registerAndLoginAndGetToken(String email, String password, String name) {
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

    @Step("Обновление имени пользователя на '{newName}' с токеном авторизации")
    private void updateUserName(String token, String newName) {
        Map<String, String> updateData = new HashMap<>();
        updateData.put("name", newName);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .body(updateData)
                .when()
                .patch("/api/auth/user")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.name", equalTo(newName));
    }

    @Step("Обновление email пользователя на '{newEmail}' с токеном авторизации")
    private void updateUserEmail(String token, String newEmail) {
        Map<String, String> updateData = new HashMap<>();
        updateData.put("email", newEmail);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .body(updateData)
                .when()
                .patch("/api/auth/user")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("user.email", equalTo(newEmail));
    }

    @Step("Попытка обновления данных пользователя без авторизации с телом: {updateData}")
    private void updateUserWithoutAuth(Map<String, String> updateData) {
        given()
                .contentType(ContentType.JSON)
                .body(updateData)
                .when()
                .patch("/api/auth/user")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }

    @Test
    public void updateNameWithAuthorizationReturnsSuccess() {
        String email = "update_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";
        token = registerAndLoginAndGetToken(email, password, "Старое имя");

        updateUserName(token, "Новое имя");
    }

    @Test
    public void updateEmailWithoutAuthorizationReturnsUnauthorized() {
        Map<String, String> updateData = new HashMap<>();
        updateData.put("email", "newmail@test.ru");

        updateUserWithoutAuth(updateData);
    }

    @Test
    public void updateEmailWithAuthorizationReturnsSuccess() {
        String email = "update_email_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";
        token = registerAndLoginAndGetToken(email, password, "Имя");

        updateUserEmail(token, "new_" + email);
    }

    @Test
    public void updateNameWithoutAuthorizationReturnsUnauthorized() {
        Map<String, String> updateData = new HashMap<>();
        updateData.put("name", "Хакер");

        updateUserWithoutAuth(updateData);
    }
}