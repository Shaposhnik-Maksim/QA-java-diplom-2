import io.qameta.allure.Step;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class UserOrderTest {

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

    @Step("Регистрация и логин пользователя, получение токена")
    private String registerAndLoginAndGetToken() {
        String email = "orders_" + System.currentTimeMillis() + "@test.ru";
        String password = "123456";
        String name = "User";

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

        Response response = given()
                .contentType(ContentType.JSON)
                .body(credentials)
                .when()
                .post("/api/auth/login");

        return response.then()
                .statusCode(200)
                .extract()
                .path("accessToken");
    }

    @Step("Получение списка валидных ингредиентов")
    private List<String> getValidIngredientIds() {
        return given()
                .get("/api/ingredients")
                .then()
                .statusCode(200)
                .extract()
                .path("data._id");
    }

    @Step("Создание заказа с токеном авторизации")
    private void createOrder(String token) {
        List<String> ingredients = getValidIngredientIds();

        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", ingredients.subList(0, 2));

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(200);
    }

    @Test
    public void getUserOrdersWithAuthorizationReturnsSuccess() {
        token = registerAndLoginAndGetToken();
        createOrder(token);

        given()
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("orders", not(empty()));
    }

    @Test
    public void getUserOrdersWithoutAuthorizationReturnsUnauthorized() {
        given()
                .contentType(ContentType.JSON)
                .when()
                .get("/api/orders")
                .then()
                .statusCode(401)
                .body("success", equalTo(false))
                .body("message", equalTo("You should be authorised"));
    }
}