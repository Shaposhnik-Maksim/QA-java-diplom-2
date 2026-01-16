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

public class OrderCreationTest {

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

    @Step("Регистрация нового пользователя и получение токена авторизации")
    private String getToken() {
        String email = "order_" + System.currentTimeMillis() + "@test.ru";
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

    @Step("Создание заказа с ингредиентами: {ingredients} и токеном авторизации")
    private Response createOrderWithAuth(List<String> ingredients, String token) {
        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", ingredients);

        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", token)
                .body(body)
                .when()
                .post("/api/orders");
    }

    @Step("Создание заказа с ингредиентами: {ingredients} без авторизации")
    private Response createOrderWithoutAuth(List<String> ingredients) {
        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", ingredients);

        return given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders");
    }

    @Test
    public void createOrderWithAuthReturnsSuccess() {
        token = getToken();
        List<String> ingredients = getValidIngredientIds();

        createOrderWithAuth(ingredients.subList(0, 2), token)
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("order.number", notNullValue());
    }

    @Test
    public void createOrderWithoutAuthReturnsSuccess() {
        List<String> ingredients = getValidIngredientIds();

        createOrderWithoutAuth(ingredients.subList(0, 2))
                .then()
                .statusCode(200)
                .body("success", equalTo(true))
                .body("order.number", notNullValue());
    }

    @Test
    public void createOrderWithoutIngredientsReturnsBadRequest() {
        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", new ArrayList<>());

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(400)
                .body("success", equalTo(false))
                .body("message", equalTo("Ingredient ids must be provided"));
    }

    @Test
    public void createOrderWithInvalidIngredientHashReturnsServerError() {
        Map<String, Object> body = new HashMap<>();
        body.put("ingredients", Arrays.asList("invalid123", "fake456"));

        given()
                .contentType(ContentType.JSON)
                .body(body)
                .when()
                .post("/api/orders")
                .then()
                .statusCode(500);
    }
}