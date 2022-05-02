## [리액티브 코프링] R2DBC로 만들어보는 예제

### 0. 들어가며
> 최근 [스프링 부트 실전 활용 마스터](https://www.aladin.co.kr/shop/wproduct.aspx?ItemId=271824446) 라는 책으로 스프링 리액티브 프로그래밍을 학습했다.  
> 리액티브 프로그래밍에 블로킹으로 동작되는 코드가 있다면 병목이 발생해서 리액티브 프로그래밍의 이점이 없기 때문에
> 이 책에서는 데이터 스토어로 RDBMS(관계형 데이터베이스)가 아닌 MongoDB를 사용하고 있는데 MongoDB가 리액티브 패러다임을 지원하고 있기 때문이다.
> 여태까지 RDBMS(관계형 데이터베이스)를 비동기로 접속하는 [R2DBC](https://r2dbc.io/) 가 1.0 버전에 도달하지 못한 상태였다.  
> 하지만 [최근 R2DBC가 1.0 버전을 릴리즈 했고](https://r2dbc.io/2022/04/25/r2dbc-1.0-goes-ga) R2DBC 사양을 따르는 개별 데이타베이스 드라이버들도 곧 1.0 버전으로 올라가게 될 것으로 보인다.
> 그래서 R2DBC를 이용해 리액티브 코프링 프로젝트를 구성하는 예제를 다루고자 한다.

----

### 1. 실습 환경
- 모든 예제 코드는 필자의 [github 레포지토리](https://github.com/anomie7/spring-kotlin-reactive/tree/master/r2dbc-example) 에서 확인할 수 있다.
- h2 DB를 사용했다.
- 애플리케이션 실행시 [schema.sql](https://github.com/anomie7/spring-kotlin-reactive/blob/master/r2dbc-example/src/main/resources/schema.sql) 파일을 이용해서 테이블을 만들고 데이터를 입력하도록 설정했다.
- spring data R2DBC를 사용했다.
- 도메인 모델은 책만 출판사에서 출간한 '스프링 부트 실전 활용 마스터'의 Cart, CartItem, Item 객체와 연관관계를 차용했다.

------
### 2. 엔티티 선언하기

이번 예제에서 사용할 엔티티들을 선언한다.  
참고로, Spring Data R2DBC에서는 연관관계를 지원하지 않는다.  
객체간 연관관계를 나타내는 멤버에는 @Transient 어노테이션을 사용해줘야 어플리케이션 실행시 오류가 나지 않는다.   

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
### 2. 데이터 조회하기
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

CartCustomRepositoryImpl는 추후 join을 이용해서 연관관계를 구현하기 위해서 미리 만들었다.  


위 레포지토리의 테스트 코드를 작성해서 실행 결과를 확인하자.
정상적으로 실행되는 것을 확인할 수 있을 것이다.
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
### 3. 데이터 영속화하기

### 4. 연관 관계 구현하기

### 4. Controller로 클라이언트에 api 제공


### 부록. 추가적인 데이터 조회 방법
// Fluent API 도 있다고 업급해주기, 그러나 조인 사용떄문에 이 예제에서는 사용하지 않는다고 언급, 레퍼런스 링크 첨부   
// 쿼리 메서드 사용