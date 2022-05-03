## [리액티브 코프링] R2DBC 사용법

### 0. 들어가며
> 최근 [스프링 부트 실전 활용 마스터](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=271824446) 라는 책으로 스프링 리액티브 프로그래밍을 학습했다.  
> 리액티브로 동작하는 코드에 블로킹으로 동작하는 코드가 존재한다면 병목이 발생해서 리액티브 프로그래밍의 이점이 없어지기 때문에
> 책에서는 데이터 스토어로 RDBMS(관계형 데이터베이스)가 아닌 리액티브 패러다임을 지원하는 MongoDB를 사용하고 있다.   
> 여태까지 RDBMS(관계형 데이터베이스)를 비동기로 연결해주는 [R2DBC](https://r2dbc.io/) 가 1.0 버전에 도달하지 못한 상태였기 때문이다.   
> 하지만 [최근 R2DBC가 1.0 버전을 릴리즈했고](https://r2dbc.io/2022/04/25/r2dbc-1.0-goes-ga) R2DBC 사양을 따르는 개별 데이터베이스 드라이버들도 오래되지 않아 1.0 버전에 도달할 것으로 보인다.
> 그래서 R2DBC를 이용해 리액티브 코프링 프로젝트를 구성하는 예제를 다루고자 한다.   

----

### 1. 실습 환경
- 모든 예제 코드는 필자의 [github 레포지토리](https://github.com/anomie7/spring-kotlin-reactive/tree/master/r2dbc-example) 에서 확인할 수 있다.
- h2 DB를 사용한다.
- 애플리케이션 실행할 때 [schema.sql](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/main/resources/schema.sql) 파일을 실행시켜 DB 테이블을 만들고 데이터를 입력하도록 설정했다.
- [Spring Data R2DBC](https://spring.io/projects/spring-data-r2dbc) 를 사용했다.
- 도메인 모델은 책만 출판사에서 출간한 '스프링 부트 실전 활용 마스터'의 Cart, CartItem, Item 객체와 연관관계를 차용했다.

------
### 2. 엔티티 선언하기

이번 예제에서 사용할 엔티티들을 선언하자.  
Spring Data R2DBC에서는 연관관계를 지원하지 않는다.  
객체간 연관관계를 구성하는 멤버에는 @Transient 어노테이션을 사용해줘야 스프링 실행 시 오류가 나지 않는다.   

```kotlin
data class Cart(
    @Id
    val id: Long? = null,
    @Transient
    var cartItems: List<CartItem> = listOf()
)
```

```kotlin
data class CartItem(
    @Id
    val id: Long? = null,
    var quantity: Int = 1,
    @Column("cart_id")
    var cartId: Long? = null,
    @Column("item_id")
    var itemId: Long? = null,
    @Transient
    var item: Item
)
```

```kotlin
data class Item(
    @Id val id: Long? = null,
    var name: String,
    var price: Double
)
```
----
### 3. 데이터 조회하기
이 예제에서 다루는 데이터를 조회하는 방법은 크게 2가지이다.
1. repository 사용
   1. 기본으로 제공하는 메서드 사용
   2. @Query 사용해서 native query 사용
2. dataBaseClient 이용하기

우선 아래처럼 레포지토리를 선언해보도록 하자

```kotlin
@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository {
    @Query("select * from cart")
    fun findAllByQuery(): Flux<Cart>
}

interface CartCustomRepository {
    fun getAll(): Flux<Cart>
}

@Repository
class CartCustomRepositoryImpl(val dataBaseClient: DatabaseClient) : CartCustomRepository {
    override fun getAll(): Flux<Cart> {
        return dataBaseClient.sql("""
            SELECT * FROM cart
        """).fetch().all().map {
            val id = it["id"] as Long
            Cart(id)
        }
    }

}
```
cart 엔티티를 모두 불러오는 로직들을 구현했다.  
CartCustomRepositoryImpl는 코드가 장황한데 추후 join을 이용해서 연관관계를 구현하기 위해서 미리 만들었다.  

위 레포지토리의 테스트 코드를 작성해서 실행 결과를 확인해보도록 하자.   
필자가 작성한 테스트 코드는 아래와 같다.   
```kotlin
@SpringBootTest
class CartRepositoryTest {
    @Autowired
    private lateinit var cartRepository: CartRepository

    @Test
    fun getAllTest() {
        cartRepository.getAll()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }

    @Test
    fun findAllByQueryTest() {
        cartRepository.findAllByQuery()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }

    @Test
    fun findAllTest() {
        cartRepository.findAll()
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }.verifyComplete()
    }
}
```
   
-----
### 4. 다이내믹 쿼리(dynamic query)로 데이터 조회하기
개발할 때 다이내믹 쿼리(dynamic query)를 사용해야 하는 경우가 무조건 생기게되는데.
이를 위해 Spring Data R2DBC에서는 Query-by-Example (QBE)라는 기능을 제공한다.
QBE를 사용하기 위해서는 아래 코드와 같이 레포지토리가 ReactiveQueryByExampleExecutor를 구현해야한다.

```kotlin
@Repository
interface ItemRepository : ReactiveCrudRepository<Item, Long>, ReactiveQueryByExampleExecutor<Item>
```

아래는 QBE를 테스트하는 코드이다.
꼭 직접 실행해서 결과를 확인해보도록 하자

```kotlin
@SpringBootTest
class ItemRepositoryTests {

    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun testDynamicQueryByObject() {
        val name = "Alf alarm clock"
        val price = 19.99
        val example = Example.of(Item(name = name, price = price))
        itemRepository.findAll(example)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(it.name, name)
                Assertions.assertEquals(it.price, price)
                true
            }
            .verifyComplete()
    }

    @Test
    fun testDynamicQueryByMatcher() {
        val name = "IPhone"
        val price = 0.0
        val matcher = ExampleMatcher.matching()
            .withMatcher("name", contains().ignoreCase())
            .withIgnorePaths("price")
        val example = Example.of(Item(name = name, price = price), matcher)

        itemRepository.findAll(example)
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                Assertions.assertTrue(it.name.contains(name))
                true
            }
            .verifyComplete()
    }
}
```
Spring Data에서 제공하는 기능이므로 간편하게 추가 기능 없이 사용할 수 있다는 장점이 있다.  
다만, 항상 엔티티 객체를 생성해야 한다는 단점이 있다.  
그리고 실무의 복잡한 요구사항에 대응 못하는 상황이 있을 수 있다.  
그래서 필자는 dataBaseClient를 이용해서 직접 다이내믹 쿼리를 구성하는 예제를 구성해보았다.  

```kotlin
@Repository
class ItemCustomRepositoryImpl(
   private val dataBaseClient: DatabaseClient
) : ItemCustomRepository {
   override fun searchItem(item: Item): Flux<MutableMap<String, Any>> {
      var selectQuery = "SELECT * FROM item "
      val whereClause = mutableListOf<String>()
      if (item.name.isNotEmpty() || item.price != 0.0) {
         if (item.name.isNotEmpty()) {
            whereClause.add("UPPER(item.name) like UPPER('%${item.name}%')")
         }

         if (item.price != 0.0) {
            whereClause.add("item.price = ${item.price}")
         }
         selectQuery += whereClause.joinToString(" AND ", "WHERE ")
      }
      return dataBaseClient.sql(selectQuery).fetch().all()
   }
}
```
#### [전체 코드 참고](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/main/kotlin/com/spring/kotlin/reactive/r2dbc/repository/ItemRepository.kt)   

QBE에 비해서 코드가 장황해졌다.   
그리고 쿼리 결과를 Map 데이터 타입으로 받기 때문에 원하는 값을 얻기 위해서 매핑 로직을 별도로 구현해야 하는 번거로움이 있다.   
그렇다면 위 코드는 어떤 장점이 있을까?   
1. Item 엔티티에 무조건 의존하지 않아도 된다 (위 예제에서는 Item 엔티티를 파라미터로 사용했지만, 사용자 정의 DTO를 사용할 수도 있다.)
2. 네이티브 쿼리를 직접 작성하기 때문에 개발자의 선택지가 다양해진다. (Spring Data의 스펙에 의존하지 않는다.)

비즈니스 요구사항은 복잡하고 예측할 수 없기 때문에 코드가 장황해지더라도 네이티브 쿼리를 구성하는 것도 나쁜 선택이 아니라고 생각한다. 
목적과 의도에 맞게 코드를 구성해서 사용하도록 하자.   

아래는 위 코드를 테스트하는 코드이다.   
꼭 직접 실행해서 결과를 확인해보도록 하자   

```kotlin
@SpringBootTest
class ItemRepositoryTests {

    @Autowired
    private lateinit var itemRepository: ItemRepository


   @Test
   fun testSearchByName() {
      val iphone = "IPHONE"
      val item = Item(name = iphone, price = 0.0)
      itemRepository.searchItem(item)
         .`as`(StepVerifier::create)
         .thenConsumeWhile {
            val name = it["name"] as String
            Assertions.assertTrue(name.uppercase().contains(iphone.uppercase()))
            true
         }
         .verifyComplete()
   }

   @Test
   fun testSearchByPrice() {
      val price = 20.99
      val item = Item(name = "", price = price)
      itemRepository.searchItem(item)
         .`as`(StepVerifier::create)
         .thenConsumeWhile {
            Assertions.assertEquals(it["price"] as Double, price)
            true
         }
         .verifyComplete()
   }

   @Test
   fun testSearchByNameAndPrice() {
      val iphone = "iphone"
      val price = 20.99
      val item = Item(name = iphone, price = price)
      itemRepository.searchItem(item)
         .`as`(StepVerifier::create)
         .thenConsumeWhile {
            val name = it["name"] as String
            Assertions.assertTrue(name.uppercase().contains(iphone.uppercase()))
            Assertions.assertEquals(it["price"] as Double, price)
            true
         }
         .verifyComplete()
   }
}
```
-----
### 부록. 추가적인 데이터 조회 방법
필자가 작성한 예제 이외에도 데이터 조회를 위한 방법이 몇 가지 더 있다.      
간단하게 알아보도록 하자.   

#### 1. 쿼리 메서드
Spring Data JPA를 사용해봤다면 익숙한 기능일 것이다.   
아래 코드와 같이 레포지토리에 Spring Data에서 지원하는 규칙에 맞게 키워드를 조합한 함수명으로 쿼리문 작성을 대신할 수 있다.
```kotlin
@Repository
interface ShopRepository : R2dbcRepository<Shop, String> {

    fun findByName(name: String): Flux<Shop>

    fun findFirstByName(name: String): Mono<Shop>
}
```
[코드 출처: 카카오 헤어 git repository ](https://github.com/kakaohairshop/spring-r2dbc-study/blob/main/src/main/kotlin/kr/co/hasys/springr2dbcstudy/shop/ShopRepository.kt)

관련 공식 문서 링크
  1. https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/#repositories.query-methods
  2. https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/#appendix.query.method.subject

#### 2. Fluent API
Fluent API를 이용하면 IDE의 자동 완성 기능을 사용해서 쿼리를 작성할 수도 있다.   
여러 연산을 연결해서 코드를 작성할 수 있다는 장점이 있다.   
Fluent API는 join을 지원하지 않아서 필자는 사용하지 않았다.   
아래 코드를 보도록 하자.   

```java
public class PostRepository {

   private final R2dbcEntityTemplate template;

   public Flux<Post> findByTitleContains(String name) {
      return this.template.select(Post.class)
              .matching(Query.query(where("title").like("%" + name + "%")).limit(10).offset(0))
              .all();
   }

   public Flux<Person> findAll(String name) {
      return Flux<Person> people = template.select(Person.class)
              .all();
   }

   public Flux<Person> findByFirstNameAndLastNameSortDesc(String name) {
      Mono<Person> first = template.select(Person.class)
              .from("other_person")
              .matching(query(where("firstname").is("John")
                      .and("lastname").in("Doe", "White"))
                      .sort(by(desc("id"))))
              .one();
   }
}
```
관련 공식 문서 링크
  1. https://docs.spring.io/spring-data/r2dbc/docs/current/reference/html/#r2dbc.entityoperations.fluent-api

---

## 참고 자료
- https://github.com/kakaohairshop/spring-r2dbc-study
- https://github.com/hantsy/spring-r2dbc-sample
- https://github.com/spring-projects/spring-data-examples/tree/main/r2dbc
- [Spring Data R2DBC - Query-by-Example (QBE) example](https://github.com/spring-projects/spring-data-examples/tree/main/r2dbc/query-by-example)
### 3. 데이터 영속화하기

### 4. 연관 관계 구현하기

### 4. Controller로 클라이언트에 api 제공


