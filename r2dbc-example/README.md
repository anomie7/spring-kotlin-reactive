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

### 데이터 조회하기 참고 자료
- https://github.com/kakaohairshop/spring-r2dbc-study
- https://github.com/hantsy/spring-r2dbc-sample
- https://github.com/spring-projects/spring-data-examples/tree/main/r2dbc
- [Spring Data R2DBC - Query-by-Example (QBE) example](https://github.com/spring-projects/spring-data-examples/tree/main/r2dbc/query-by-example)
---
이전글 : [[리액티브 코프링] R2DBC 사용법 (데이터 조회)](https://anomie7.tistory.com/93)

> 모든 예제 코드는 필자의 [github 레포지토리](https://github.com/anomie7/spring-kotlin-reactive/tree/master/r2dbc-example) 에서 확인할 수 있다.
### 데이터 저장하기
이번 글에서 다루는 데이터를 저장하는 방법은 아래와 같다.

1. 레포지토리의 save() 함수 사용
2. Fluent API 사용
3. Native Query 사용
4. Batch Insert 

먼저, 1, 2, 3번에 해당하는 코드를 보도록 하자.   
각각의 테스트 코드 위에 주석으로 데이터를 저장하는 방법을 명시했다.    
</br>
```kotlin
@SpringBootTest
class ItemSaveTest {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Autowired
    private lateinit var dataBaseClient: DatabaseClient

    @Test
    // 1. 레포지토리의 save() 함수 사용
    fun saveTest() {
        val item = Item(name = "테스트 아이템", price = 110.0)
        itemRepository.save(item)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(item.name, it.name)
                Assertions.assertEquals(item.price, it.price)
                true
            }
            .verifyComplete()
    }

    @Test
    // 2. Fluent API 사용
    fun saveByTemplateTest() {
        val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)
        val item = Item(name = "테스트 아이템", price = 110.0)
        r2dbcEntityTemplate.insert(Item::class.java)
            .using(item)
            .`as`(StepVerifier::create)
            .expectNextMatches {
                Assertions.assertEquals(item.name, it.name)
                Assertions.assertEquals(item.price, it.price)
                true
            }
            .verifyComplete()
    }

    @Test
    // 3. native query 사용
    fun saveByQueryTest() {
        val id = 999L
        dataBaseClient.sql("INSERT INTO item(id, name, price) VALUES ($id, '테스트 아티템 15', 22.99)")
            .fetch().all()
            .`as`(StepVerifier::create)
            .then {
                itemRepository.findById(id)
                    .`as`(StepVerifier::create)
                    .expectNextMatches {
                        Assertions.assertEquals(id, it.id)
                        true
                    }.verifyComplete()
            }
            .verifyComplete()
    }

}
```
[전체 코드 링크](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/test/kotlin/com/spring/kotlin/reactive/r2dbc/repository/ItemSaveTest.kt)
</br>
위에서부터 차례로 보자.
1. 레포지토리의 save() 함수 사용
   1. JPA를 사용해봤다면 익숙한 방법이다.
   2. 이 기능을 사용하기 위해서 엔티티마다 repository를 선언해 줘야 한다는 단점이 있다.
2. Fluent API 사용
   1. 데이터 조회 편에서 다룬 Fluent API를 사용하고 있다.
   2. IDE 자동 완성 기능을 사용할 수 있다는 장점이 있다.
   3. Custom Repository, Custom DAO를 구현할 때 사용하기 유용하다.
3. native query 사용
   1. native query를 개발자가 작성해서 사용하는 방법이다.
   2. 반환 값이 Map 데이터타입이라 매핑 로직을 개발자가 직접 구현해야 한다.

필자는 1번은 Item 엔티티를 저장할 때 사용한다.   
왜냐하면, Item을 저장하는 기능은 Item 엔티티를 영속화하는 것만으로 끝나기 때문이다.  
2번은 Cart를 저장할 때 사용한다.   
이후 연관 관계 구현하기에서 자세히 다루겠지만, Cart를 저장하는 기능은 Cart뿐만 아니라 CartItem이라는 엔티티도 같이 영향을 받게 된다.   
그리고 CartItem에 대한 변경 점은 Cart에 종속시키고 싶기 때문에 CartItem의 repository를 별도로 만들지 않았다.   
그래서 연관 관계를 구현할 때 customRepository를 구현하고 Fluent API 사용할 것이다.     
</br>
----
### Batch Insert
Batch Insert는 여러 개의 insert 문을 모아서 한 번에 처리하는 기능이다.   
하나의 트랜잭션에서 수백 건의 row를 insert 한다고 가정해보자.   
이때 개별 row마다 insert를 수행하면 처리 시간이 오래 걸리고 불필요한 네트워크 자원이 소모된다.   
만약, 트랜잭션이 걸려있다면 해당 테이블에 lock도 걸릴 것이다.   
Batch Insert는 위의 단점을 극복하고자 여러 건의 insert 문을 하나의 구문(Statement)으로 모아서 처리하는 기능이다.   

Item 엔티티를 Batch Insert 하는 코드를 보도록 하자.   
<br>
```kotlin
@Repository
interface ItemRepository : ReactiveCrudRepository<Item, Long>, ItemCustomRepository

interface ItemCustomRepository {
   fun batchSave(items: List<Item>): Flux<Item>
}

@Repository
class ItemCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient
) : ItemCustomRepository {
    override fun batchSave(items: List<Item>): Flux<Item> {
        return dataBaseClient.inConnectionMany { connection ->
            val statement =
                connection.createStatement("INSERT INTO item(name, price) VALUES ($1, $2)")
                    .returnGeneratedValues("id", "name", "price")
            for (item in items) {
                statement.bind(0, item.name).bind(1, item.price).add()
            }
            Flux.from(statement.execute()).flatMap { result ->
                result.map { row, r ->
                    Item(row["id"] as Long, row["name"] as String, row["price"] as Double)
                }
            }
        }
    }
}
```
[전체 코드 보기](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/main/kotlin/com/spring/kotlin/reactive/r2dbc/repository/ItemRepository.kt#L39)     

<br>

위에서부터 차례대로 살펴보도록 하자.
1. `dataBaseClient.inConnectionMany()` connection을 가져온다.
2. `connection.createStatement("INSERT INTO item(name, price) VALUES ($1, $2)")` 실행할 insert 문을 작성한다.
3. `.returnGeneratedValues("id", "name", "price")` insert 수행 결과로 생성된 item 값 중 반환받을 column을 명시해준다.
4. Item 여러 건을 insert 해야하므로 for문을 사용해서 값을 바인딩한다.
5. `statement.bind(0, item.name).bind(1, item.price).add()` 바인딩할 값의 인덱스(index)와 값을 명시해준다. 
   1. 인덱스는 0부터 시작하며, 2번의 쿼리 `VALUES ($1, $2)`에 입력하는 값이다. 
6. `statement.execute()`로 insert 문을 실행해준다.
7. `statement.execute()`의 결과값은 flatMap으로 Item 엔티티로 매핑해준다.

아래의 테스트 코드로 동작을 직접 확인해보자.
</br>
```kotlin
@SpringBootTest
class ItemSaveTest {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun batchSaveTest() {
        val item1 = Item(name = "배치 아이템1", price = 101.0)
        val item2 = Item(name = "배치 아이템2", price = 141.0)
        itemRepository.batchSave(listOf(item1, item2))
            .`as`(StepVerifier::create)
            .thenConsumeWhile {
                println("item save success: $it")
                true
            }.verifyComplete()
    }

}
```
----
### 데이터 수정하기
1. 레포지토리의 save()
   1. 저장하는 방식
2. Fluent API
3. 쿼리 메소드 (@Modifying)

우선 1, 2번에 해당하는 코드를 보도록 하자.
각각의 테스트 코드 위에 주석으로 코드 동작 설명을 작성했다.    
</br>
```kotlin
@SpringBootTest
class ItemUpdateTest {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Autowired
    private lateinit var connectionFactory: ConnectionFactory

    @Test
    // 1. 레포지토리의 save()
    fun updateByRepositoryTest() {
        val updatedName = "updated item name"
        val updatedPrice = 0.0
        // id가 1,2인 item을 불러온다.
        itemRepository.findAllById(listOf(1, 2))
            // item의 name과 price를 수정한다.
            .flatMap {
                it.name = updatedName
                it.price = updatedPrice
                Mono.just(it)
            }
           // save()를 이용해 item을 저장한다.
            .flatMap {
                itemRepository.save(it)
            }
            .log()
            .`as`(StepVerifier::create)
            .thenConsumeWhile { item ->
                Assertions.assertEquals(item.name, updatedName)
                Assertions.assertEquals(item.price, updatedPrice)
                true
            }.verifyComplete()
    }

    @Test
    // 2. 레포지토리의 save()
    fun updateByFluentTest() {
        val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)
        val updatedName = "updated item name"
        val updatedPrice = 0.0
       // id가 1,2인 item을 불러온다.
        itemRepository.findAllById(listOf(1, 2))
           // item의 name과 price를 수정한다.
            .flatMap {
                it.name = updatedName
                it.price = updatedPrice
                Mono.just(it)
            }
           // Fluent API를 이용해 item을 저장한다.
            .flatMap {
                r2dbcEntityTemplate.update(Item::class.java)
                    .matching(Query.query(Criteria.where("id").`is`(it.id!!)))
                    .apply(Update.update("name", it.name).set("price", it.price))
            }
            .log()
            .`as`(StepVerifier::create)
           // update가 성공적으로 진행되었는지 반환값으로 확인한다.
            .thenConsumeWhile {
                Assertions.assertNotNull(it)
                true
            }
            .then {
               // 업데이트한 item을 불러와서 값을 검증한다.
                itemRepository.findAllById(listOf(1, 2))
                    .`as`(StepVerifier::create)
                    .thenConsumeWhile { item ->
                        Assertions.assertEquals(item.name, updatedName)
                        Assertions.assertEquals(item.price, updatedPrice)
                        true
                    }.verifyComplete()
                true
            }.verifyComplete()
    }
}
```
<br>
쿼리 메소드 (@Modifying)를 사용하는 방식은 itemRepository에 메소드를 추가해야한다.  
<br>

```kotlin
@Repository
interface ItemRepository : ReactiveCrudRepository<Item, Long>, ReactiveQueryByExampleExecutor<Item>,
    ItemCustomRepository {

    @Modifying
    @Query("UPDATE item SET name = :name, price = :price where id = :id")
    fun updateItem(name: String, price: Double, id: Long): Mono<Int?>
}
```
<br>

추가된 메소드의 특징은 아래와 같다.
1. @Query 어노테이션으로 update문을 추가했다.  
2. @Modifying를 메소드 위에 명시해줬다.   
3. 메소드의 파라미터는 update문의 값과 매핑된다.

테스트 코드를 이용해서 결과를 확인해보도록 하자.

<br>

```kotlin
@SpringBootTest
class ItemUpdateTest {
    @Autowired
    private lateinit var itemRepository: ItemRepository

    @Test
    fun updateByModifyingQueryTest() {
        val updatedName = "updated item name"
        val updatedPrice = 0.0
        val itemId: Long = 1
        // update할 item의 정보를 입력해준다.
        itemRepository.updateItem(updatedName, updatedPrice, itemId)
            .log()
            .`as`(StepVerifier::create)
            // update가 성공적으로 진행되었는지 반환값으로 확인한다.
            .expectNextMatches {
                Assertions.assertNotNull(it)
                true
            }
            .then {
               // 업데이트한 item을 불러와서 값을 검증한다.
                itemRepository.findById(itemId)
                    .`as`(StepVerifier::create)
                    .thenConsumeWhile { item ->
                        Assertions.assertEquals(item.name, updatedName)
                        Assertions.assertEquals(item.price, updatedPrice)
                        true
                    }.verifyComplete()
                true
            }.verifyComplete()
    }
}
```
----
#### 데이터 수정하기 참고 자료
- [batching](https://r2dbc.io/spec/1.0.0.RELEASE/spec/html/#statements.batching)
- [Fluent Api multiple columns update](https://github.com/spring-projects/spring-data-r2dbc/issues/195)
- https://docs.spring.io/spring-data/r2dbc/docs/1.2.2/reference/html/#r2dbc.entityoperations.fluent-api.insert
----
이전글 : [[리액티브 코프링] R2DBC 사용법 ((데이터 저장 & 수정)](https://anomie7.tistory.com/94)

> 모든 예제 코드는 필자의 [github 레포지토리](https://github.com/anomie7/spring-kotlin-reactive/tree/master/r2dbc-example) 에서 확인할 수 있다.
### 4. 연관 관계 구현하기
> R2DBC는 JPA같은 ORM이 아니므로 연관 관계 매핑을 지원하지 않는다.
> R2DBC에서 연관 관계 매핑과 같은 기능을 사용하기 위해서는 개발자가 추가적으로 코드를 작성해줘야한다.
<br>
#### 도메인 & 요구사항
본 예제에서는 [스프링 부트 실전 활용 마스터](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=271824446) 의 장바구니 예제를 차용하고 있다.   
Cart, CartItem, Item 엔티티를 사용하고 있으며 연관 관계는 아래와 같다.   

<br>

![도메인 연관관계](https://github.com/anomie7/spring-kotlin-reactive/blob/master/images/Untitled%20Diagram.drawio.png?raw=true)

<br>

Cart와 CartItem은 1대 N 연관관계이고, CartItem과 Item은 일대일 연관관계이다.

-----
#### 엔티티 설정 (Transient)
데이터 조회편에서 엔티티를 선언했지만 연관 관계 구현을 위해 추가적으로 엔티티를 설정해줘야한다.   

<br>

```kotlin
data class Cart(
    @Id
    val id: Long? = null,

    @Transient
    @Value("null")
    var cartItems: List<CartItem>? = null
)

data class CartItem(
   @Id
   val id: Long? = null,
   var quantity: Int = 1,
   @Column("cart_id")
   var cartId: Long? = null,
   @Column("item_id")
   var itemId: Long? = null,

   @Transient
   @Value("null")
   var item: Item? = null
) {
   fun increment() {
      this.quantity += 1
   }
}

data class Item(
   @Id val id: Long? = null,
   var name: String,
   var price: Double
)
```
<br>

이전의 엔티티와 차이가 있는데, @Transient 어노테이션 아래에 @Value("null")를 명시했다.   
코틀린에서 R2DBC를 사용할 떄 @Transient를 적용한 프로퍼티에 기본값을 null로 할당했더라도 엔티티를 불러올 때 null값이 들어가지 않는 문제가 있다.   
그래서 Cart에 cartItems가 없는 상황을 위해 @Value를 이용해서 null값을 넣어줘야한다.   
----
#### 조회
엔티티 수정했다면 Cart를 조회할 때 cartItems도 같이 조회하는 코드를 작성해보자.   

<br>

```kotlin
@Repository
interface CartRepository : ReactiveCrudRepository<Cart, Long>, CartCustomRepository

interface CartCustomRepository {
    fun getAll(): Flux<Cart>
    fun getById(cartId: Long): Flux<Cart>
}

@Repository
class CartCustomRepositoryImpl(
    private val dataBaseClient: DatabaseClient,
    connectionFactory: ConnectionFactory
) : CartCustomRepository {
    private val r2dbcEntityTemplate = R2dbcEntityTemplate(connectionFactory)

    private val cartMapper: (t: MutableList<MutableMap<String, Any>>) -> Cart
        get() {
            val cartMapper: (t: MutableList<MutableMap<String, Any>>) -> Cart = { list ->
                val cartId = list[0]["cart_id"] as Long
                val cartItems = list.stream().map {
                    val id = it["id"] as Long
                    val quantity = it["quantity"] as Int
                    val cartId = it["cart_id"] as Long
                    val itemId = it["item_id"] as Long
                    val name = it["item_name"] as String
                    val price = it["item_price"] as Double
                    CartItem(
                        id = id,
                        quantity = quantity,
                        cartId = cartId,
                        itemId = itemId,
                        Item(
                            id = itemId,
                            name = name,
                            price = price
                        )
                    )
                }.collect(Collectors.toList())
                Cart(id = cartId, cartItems = cartItems)
            }
            return cartMapper
        }

    override fun getAll(): Flux<Cart> {
        return dataBaseClient.sql(
            """
            SELECT cart_item.*, item.name as item_name, item.price as item_price FROM cart
            INNER JOIN cart_item ON cart.id = cart_item.cart_id
            INNER JOIN item ON cart_item.item_id = item.id
        """
        ).fetch().all()
            .bufferUntilChanged {
                it["cart_id"]
            }.map(cartMapper)
    }

    override fun getById(cartId: Long): Flux<Cart> {
        return dataBaseClient.sql(
            """
                SELECT cart_item.*, item.name as item_name, item.price as item_price FROM cart_item
                INNER JOIN item ON cart_item.item_id = item.id
                WHERE cart_item.cart_id = :cart_id
            """.trimMargin()
        )
            .bind("cart_id", cartId)
            .fetch().all()
            .bufferUntilChanged {
                it["cart_id"]
            }.map(cartMapper)
    }
}
```
<br>

##### getAll() 동작 설명
1. dataBaseClient.sql()를 이용해서 쿼리를 실행한다. 
2. 쿼리문은 cart, cart_item, item를 조인해서 item의 칼럼(name, price)과 cart_item의 칼럼(id, quantity, cart_id, item_id)을 불러온다.
3. .fetch().all()로 쿼리를 실행하고 결과를 불러온다.
4. bufferUntilChanged()를 이용해서 불러온 row들을 cart_id 기준으로 묶어서 Flux<List<Map>> 형태로 변환해준다.
5. map()을 이용해서 Map 형태로 받은 데이터를 엔티티로 변환해준다.

##### getById() 동작 설명
1. dataBaseClient.sql()를 이용해서 쿼리를 실행한다.
2. 쿼리문은 cart_item, item를 조인해서 item의 칼럼(name, price)과 cart_item의 칼럼(id, quantity, cart_id, item_id)을 불러온다.
3. bind()로 쿼리문의 where 절에 넣을 cart_id 값을 입력해준다.
4. bufferUntilChanged()를 이용해서 불러온 row들을 cart_id 기준으로 묶어서 Flux<List<Map>> 형태로 변환해준다.
5.  map()을 이용해서 Map 형태로 받은 데이터를 엔티티로 변환해준다.

참고로, map()에서 사용하는 로직은 공통이라 cartMapper를 별도 선언해서 사용하고 있다.   
그리고 bufferUntilChanged() 동작이 궁금하다면 본 글의 최하단 **bufferUntilChanged() 관련 참고 자료**를 참고하라.
----
#### 저장
장바구니에 아이템을 넣는 동작을 구현해보도록 하자.
장바구니에 아이템을 넣는 동작은 두가지 경우로 나뉜다.
1. 장바구니에 넣을 아이템이 있는 경우
2. 장바구니에 넣을 아이템이 없는 경우

1.의 경우에는 CartItem의 quantity만 1 증가시키면 된다.   
2.의 경우에는 CartItem를 새로 생성하면서 quantity 값은 1로 초기화해준다.

위 동작을 구현한 코드를 보도록 하자

<br>

```kotlin
    override fun addItemToCart(cartId: Long, item: Item): Flux<CartItem> {
        // 1. 먼저 정의한 getById()로 cart를 조회한다.
        return getById(cartId)
            // 2. 만약 결과가 없다면 switchIfEmpty()로 Exception을 던진다.
            .switchIfEmpty(Mono.error(RuntimeException("[cart not founded $cartId]")))
            .flatMap { cart ->
               // 3. 조회한 cart에서 추가할 item을 담고있는 CartItem을 찾는다. 만약, 없다면 새로운 CartItem을 생성한다.
                val cartItem = cart.cartItems?.firstOrNull { it.itemId == item.id }
                    ?: CartItem(
                        cartId = cartId,
                        itemId = item.id,
                        quantity = 0,
                        item = item
                    )
                // 4. quantity를 1 증가시킨다.
                cartItem.increment()
                Mono.just(cartItem)
            }.flatMap { cartItem ->
                val id = cartItem.id
                // 5-1. cartItem에 id가 있다면 update문으로 quantity 칼럼값을 업데이트한다.
                if (id != null) {
                    r2dbcEntityTemplate.update(CartItem::class.java)
                        .matching(
                            org.springframework.data.relational.core.query.Query.query(
                                Criteria.where("id").`is`(id)
                            )
                        )
                        .apply(Update.update("quantity", cartItem.quantity))
                        .flatMap {
                            Mono.just(cartItem)
                        }
                // 5-2. cartItem에 id가 없다면 insert 문으로 cartItem을 생성한다.
                } else {
                    r2dbcEntityTemplate.insert(CartItem::class.java)
                        .using(cartItem)
                }
            }
    }
```
[전체 코드 보기](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/main/kotlin/com/spring/kotlin/reactive/r2dbc/repository/CartRepository.kt)

<br>

##### 동작 설명
1. 먼저 정의한 getById()로 cart를 조회한다.
2. 만약 결과가 없다면 switchIfEmpty()로 Exception을 던진다.
3. 조회한 cart에서 추가할 item을 담고있는 CartItem을 찾는다. 만약, 없다면 새로운 CartItem을 생성한다.
4. quantity를 1 증가시킨다.
5. 변경된 값을 DB에 반영한다.
   1. cartItem에 id가 있다면 update문으로 quantity 칼럼값을 업데이트한다.
   2. cartItem에 id가 없다면 insert 문으로 cartItem을 생성한다.
----
#### 컨트롤러 구현
연관 관계 매핑 구현은 완료했다.   
아래는 위 로직을 컨트롤러에 제공하기 위해 선언한 Service 객체의 코드이다.

<br>

````kotlin
@Service
class CartService(
    val cartRepository: CartRepository,
    val itemRepository: ItemRepository
) {
    fun getAll(): Flux<Cart> {
        return cartRepository.getAll()
    }

    fun getById(cartId: Long): Flux<Cart> {
        return cartRepository.getById(cartId)
    }

    fun addItem(cartId: Long, itemId: Long): Flux<CartItem> {
        return itemRepository.findById(itemId)
            .switchIfEmpty(Mono.error(RuntimeException("item not founded $itemId")))
            .flatMapMany { item ->
                cartRepository.addItemToCart(cartId, item)
            }
    }
}
````
<br>

아래는 위 코드에 의존하고 있는 컨트롤러 코드이다.

<br>

```kotlin
@RestController
class CartController(val cartService: CartService) {

    @GetMapping("v1/carts")
    fun getCarts(): Flux<Cart> {
        return cartService.getAll()
    }

    @GetMapping(value = ["v1/carts/stream"], produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun getCartsByStream(): Flux<Cart> {
        return cartService.getAll()
    }

    @GetMapping("v1/carts/{id}")
    fun getCartsById(@PathVariable("id") id: Long): Flux<Cart> {
        return cartService.getById(id)
    }

    @PostMapping("v1/carts/{id}/add/{itemId}")
    fun addItem(@PathVariable("id") cartId: Long, @PathVariable("itemId") itemId: Long): Flux<CartItem> {
        return cartService.addItem(cartId, itemId)
    }
}
```
----
#### 연관 관계 구현하기 참고 자료
- https://javacan.tistory.com/entry/Reactor-Start-9-window-buffer
- https://www.vinsguru.com/reactor-buffer-vs-window/

#### bufferUntilChanged() 관련 참고 자료
- https://heesutory.tistory.com/34?category=901813
- https://www.sipios.com/blog-tech/handle-the-new-r2dbc-specification-in-java

