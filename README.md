# stock
해당 문서 출처는 [재고시스템으로 알아보는 동시성이슈 해결방법](https://www.inflearn.com/course/%EB%8F%99%EC%8B%9C%EC%84%B1%EC%9D%B4%EC%8A%88-%EC%9E%AC%EA%B3%A0%EC%8B%9C%EC%8A%A4%ED%85%9C) 기반으로 작성되었습니다. 

## 재고시스템 만들어보기
### 재고감소 로직 문제점
![image](https://github.com/haeyonghahn/stock/assets/31242766/f4b93643-e012-4a8e-b975-e91875dfdd3a)

레이스 컨디션이란 둘 이상의 Thread가 공유 데이터에 액세스할 수 있고 동시에 변경을 하려고 할 때 발생하는 문제이다. Thread1이 데이터를 가져가서 갱신을 한 값을 Thread2가 가져간 후에 갱신을 하는 것을 예상했다. 
하지만 실제로는 Thread1이 데이터를 가져가서 갱신을 하기 이전에 Thread2가 가져가서 갱신되기 전에 값을 가져가게 된다. 그리고 Thread1이 갱신을 하고 Thread2도 갱신을 하지만 둘 다 재고가 5인 상태에서 1을 줄인 값을 갱신
하기 때문에 갱신이 누락되게 된다. 이렇게 두 개 이상의 스레드가 공유 데이터에 액세스를 할 수 있고 동시에 변경을 하려고 할 때 발생하는 문제를 레이스 컨디션이라고 한다. 이런 문제를 해결하기 위한 여러가지 방법을 알아보자.

## Synchronized 이용해보기
자바에서 Synchronized를 활용하면 손쉽게 한 개의 스레드만 접근이 가능하도록 할 수 있다. Synchronized를 메소드 선언부에 붙여주게 된다면 해당 메소드는 한 개의 Thread만 접근이 가능하게 된다. 하지만 Synchronized를 활용했음에도 불구하고 테스트 케이스가 실패하는 것을 확인할 수 있다. 이것은 스프링의 Transactional Annotation의 동작 방식 때문이다. 스프링에서는 Transactional Anntation을 이용하면 우리가 만든 클래스를 래핑한 클래스를 새로 만들어서 실행하게 된다. 트랜잭션을 시작한 후에 메소드를 호출하고 메소드 실행이 종료된다면 트랜잭션을 종료하게 된다. 여기서 문제가 발생한다. 트랜잭션 종료 시점에 데이터베이스에 업데이트를 하는데, decrease 메소드가 완료되었고 실제 데이터베이스가 업데이트되기 전에 다른 Thread가 decrease 메소드를 호출할 수 있다. 그렇게 되면 다른 Thread가 갱신되기 전에 값을 가져가서 이전과 동일한 문제가 발생하는 것이다. 다양한 해결 방법이 있겠지만 이번에는 Transaction Annotation을 주석처리하고 다시 테스트해보면 성공하는 것을 볼 수 있다.

### Synchronized 문제점
![image](https://github.com/haeyonghahn/stock/assets/31242766/d6184b51-9d49-40a4-9f20-12ffdbad16c2)

Java의 Synchronized는 하나의 프로세스 안에서만 보장된다. 서버가 1대일 때는 데이터의 접근을 서버가 1대만 해서 괜찮지만, 서버가 2대 혹은 그 이상일 경우는 데이터의 접근을 여러 대에서 할 수 있게 된다. 예를 들어 Server-1에서 10시에 재고감소 로직을 시작하고 10시 5분에 종료하게 된다고 가정해보자. 그러면 Server-2에서 10시에서 10시 5분 사이에 갱신되지 않는 재고를 가져가서 새로운 값으로 갱신할 수 있게 된다. Synchronized는 각 프로세스 안에서만 보장이 되기 때문에 결국 여러 Thread에서 동시에 데이터에 접근할 수 있게 되면서 레이스 컨디션이 발생하게 된다. 실제 운영 중인 서비스는 대부분 2대 이상의 서버를 사용하기 때문에 Synchronized를 거의 사용하지 않는다. 이런 문제를 해결하기 위해 MySQL이 지원해주는 방법으로 문제를 해결해보자.

## Database 이용해보기
- Pessimistic Lock
  - 실제로 데이터에 Lock 을 걸어서 정합성을 맞추는 방법이다. exclusive lock 을 걸게되며 다른 트랜잭션에서는 lock 이 해제되기전에 데이터를 가져갈 수 없게된다. 데드락이 걸릴 수 있기때문에 주의하여 사용해야 한다.
- Optimistic Lock
  - 실제로 Lock 을 이용하지 않고 버전을 이용함으로써 정합성을 맞추는 방법이다. 먼저 데이터를 읽은 후에 update 를 수행할 때 현재 내가 읽은 버전이 맞는지 확인하며 업데이트 한다. 내가 읽은 버전에서 수정사항이 생겼을 경우에는 application에서 다시 읽은후에 작업을 수행해야 한다.
- Named Lock
  - 이름을 가진 metadata locking 이다. 이름을 가진 lock 을 획득한 후 해제할때까지 다른 세션은 이 lock 을 획득할 수 없도록 한다. 주의할 점으로는 transaction 이 종료될 때 lock 이 자동으로 해제되지 않는다. 별도의 명령어로 해제를 수행해주거나 선점시간이 끝나야 해제된다.

### Pessimistic Lock 활용해보기
![image](https://github.com/haeyonghahn/stock/assets/31242766/4a06b688-d19c-4ee6-b2c1-55bb9a5349b5)

Thread1이 락을 걸고 데이터를 가져간다. 그리고 그 후에 Thread2가 락을 걸고 데이터 획득을 시도한다. Thread1이 이미 점유 중이므로 잠시 대기하게 된다. Thread1의 작업이 모두 완료가 되면 Thread2가 락을 걸고 데이터를 가져갈 수 있게 된다. Pessimistic Lock 장점은 충돌이 빈번하게 일어난다면 Optimistic Lock보다 성능이 좋을 수 있다. 두 번째로는 락을 통해 업데이트를 제어하기 때문에 데이터 정합성이 보장된다. 단점으로는 별도의 락을 잡기 때문에 성능 감소가 있을 수 있다.

### Optimistic Lock 활용해보기
![image](https://github.com/haeyonghahn/stock/assets/31242766/4415e41f-dae0-4e16-bdeb-54945d105f19)

서버 1과 서버 2가 버전이 1인 데이터를 가져간다. 그리고 서버 1이 업데이트할 때 버전을 1개 올려준다. 그 후에 서버가 버전이 1인 조건을 가지고 업데이트를 시도한다. 하지만 현재 데이터의 버전은 2이기 때문에 업데이트는 실패하게 된다. 이것처럼 내가 읽은 버전에서 수정사항이 생겼을 경우 애플리케이션에서 다시 읽은 후에 작업을 수행해야 한다. 해당 목차에서는 소스상에 먼저, Optimistic Lock을 사용하기 위해서 버전 컬럼을 추가해 주어야 한다. 그리고 version 어노테이션을 붙인다. Optimistic Lock은 실패했을 때 재시도를 해야함으로 Facade라는 클래스를 생성하도록 한다. Optimistic Lock의 장점으로는 별도의 락을 잡지 않으므로 Pessimistic Lock보다 성능상 이점이 있다. 단점으로는 업데이트가 실패했을 때 재시도 로직을 개발자가 직접 작성해주어야 하는 번거로움이 있다.

### Named Lock 활용해보기
![image](https://github.com/haeyonghahn/stock/assets/31242766/45353049-3d1e-473e-bc42-c82290bbe86b)

Named Lock은 이름을 가진 메타데이터 락이다. 이름을 가진 락을 획득한 후에 해제할 때까지 다른 세션은 락을 획득할 수 없게 된다. 주의할 점은 트랜잭션이 종료될 때 락이 자동으로 해제되지 않기 때문에 별도의 명령어로 해제를 수행하거나 선점 시간이 끝나야 락이 해제된다. MySQL에서는 get_lock 명령어를 통해 Named Lock을 획득할 수 있고 release_lock 명령어를 통해 lock을 해제할 수 있다. Pessimistic Lock은 Stock에 대해서 락을 걸었다면, Named Lock은 Stock에 lock을 걸지 않고 별도의 공간에 lock을 걸게 된다. Session-1이라는 이름으로 lock을 건다면 다른 Session에서는 Session1이 락을 해제한 후에 lock을 획득할 수 있게 된다. 

Named Lock을 사용할 때에는 실제로 데이터소스를 분리해서 사용해야 한다. 같은 데이터소스를 사용하면 커넥션 풀이 부족해지는 현상으로 인해 다른 서비스에 영향을 미칠 수 있다. 예제에서 편의성을 위해 Stock 엔티티에 Named Lock을 사용하지만 실무에서는 별도의 JDBC를 사용해야 한다. Stock 서비스에서는 부모의 트랜잭션과 별도로 실행이 되어야 되기 때문에 트랜잭션 전파 옵션을 변경해준다. 

Named Lock은 주로 분산락을 구현할 때 사용한다. Pessimistic Lock은 타임아웃을 구현하기 힘들지만 Named Lock은 타임아웃을 손쉽게 구현할 수 있다. 그 외에도 데이터 삽입 시 정합성을 맞춰야 하는 경우에도 Named Lock을 사용할 수 있다. 하지만 이 방법은 트랜잭션 종료 시에 락 해제, 세션 관리를 잘 해줘야 하기 때문에 주의해서 사용해야 하고 실제로 사용할 때는 구현 방법이 복잡할 수 있다.

## Redis 이용해보기
분산락을 구현할 때 사용하는 대표적인 라이브러리는 Lettuce와 Redisson이 있다. 첫 번째로 Lettuce는 setNx 명령어를 활용하여 분산락을 구현할 수 있다. setNx 명령어는 `set if not exist`의 줄임말로 key와 value를 set할 때 값이 없을 때만 set하는 명령어이다. setNx를 활용하는 방식은 SpinLock 방식이므로 retry로직을 개발자가 직접 작성해주어야 한다. `SpinLock`이란 락을 획득하려는 Thread가 락을 사용할 수 있는지 반복적으로 확인하면서 락 획득을 시도하는 방식이다. 

![image](https://github.com/haeyonghahn/stock/assets/31242766/f37a5368-92f3-4ca8-b67c-52da59f43218)

Thread1이 key가 1인 데이터가 없기 때문에 정상적으로 set하게 되고 Thread1의 성공을 리턴하게 된다. 그 후에 Thread2가 똑같이 key가 1인 데이터를 set하려고 할 때 레디스에는 이미 키가 1인 데이터가 있으므로 실패를 리턴하게 된다. Thread2는 lock 획득에 실패했기 때문에 일정 시간 이후에 lock 획득을 재시도한다. 락 획득할 때까지 재시도하는 로직을 작성해주어야 한다. 

두 번째로, Redisson같은 경우 pub/sub 기반의 lock 구현이 되어 있다. pub/sub 기반은 채널을 하나 만들고 lock을 점유 중인 Thread가 lock 획득하려고 대기중인 Thread에게 해제를 알려주면 안내를 받은 Thread가 lock 획득을 시도하는 방식이다. 해당 방식은 Lettuce와 다르게 별도의 retry로직을 작성하지 않아도 된다. 

![image](https://github.com/haeyonghahn/stock/assets/31242766/4335b45e-14a3-4e35-b28e-dee229dcc5ed)

채널이 하나 존재하고 Thread1이 먼저 lock을 점유하고 Thread2가 이후에 시도를 하려고 한다면 Thread1이 lock을 해제할 때 '나 끝났어'라는 메세지를 채널로 보내게 된다. 그러면 채널은 Thread2의 lock 획득을 시도해라는 것을 알려주고 Thread2는 lock 획득을 시도하게 된다.

### Lettuce를 작성하여 재고감소 로직 작성하기
Redis의 Lettuce는 Named Lock과 비스하지만 다른 점으로는 Redis를 이용한다는 점과 Session 관리에 신경을 안 써도 된다는 점이다. Lettuce를 활용한 방법은 구현이 간단하다는 장점이 있다. 단점으로는 SpinLock 방식이므로 redis에 부하를 줄 수 있다.
