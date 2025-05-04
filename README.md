# 아이북조아 📚

> 본 리드미는 본인(이승희)이 기술적으로 기여한 기능 중심으로 작성되었습니다. <br />
> 개발기간: 2024.10.15 ~ 2024.11.03 (3주)

## _Intro._

담당한 주요 기능은 다음과 같습니다:

1. **맞춤형 콘텐츠 추천 시스템**: 자녀의 성향에 맞는 도서 콘텐츠를 추천합니다. 사용자의 좋아요/싫어요 피드백에 따라 추천 책이 익일 새벽에 변경됩니다.
2. **도서 조회**: 도서 목록과 상세 정보를 조회하고, 검색 기능을 제공합니다.
3. **도서 좋아요**: 사용자는 도서에 좋아요를 누를 수 있으며, 해당 데이터는 도서 추천 시스템에 활용됩니다.

<p align="center">
  <img src="https://github.com/user-attachments/assets/ea901338-0f76-457a-a046-7763ef2d8967">
</p>

<br />

## _Documents._

- 트러블 슈팅 및 학습한 내용
    - [추천 배치 설계와 성능 테스트 (feat. JPA IDENTITY 한계 극복)](https://velog.io/@leeseunghee00/추천-배치-설계와-성능-테스트)
    - [도서 검색 성능 향상을 위한 Full-text Search 적용](https://courageous-fluorine-f2d.notion.site/Full-text-Search-5827ada6391f4b409107a0eaee9163fb?pvs=4)
    - [Redis 간단히 알아보기](https://courageous-fluorine-f2d.notion.site/Redis-526c76b479e0474a8239e2b2bc5def09?pvs=4)
- [프로젝트 회고](https://velog.io/@leeseunghee00/LG-U-유레카-백엔드-1기를-수료하며#-페어-프로그래밍의-묘미를-알게-해-준-종합-프로젝트)
- [회의록](https://www.notion.so/1221822ed9d181789271e61afb3a5d78?v=1221822ed9d181faa86e000c9e597b39&pvs=4)
- [API 명세서](https://www.notion.so/API-1221822ed9d1816281e9c6233979f87f?pvs=4)

<br />

## _Stack._

> Backend

- Java 17, SpringBoot 3.3.4
- Spring Data JPA, JDBC
- Spring Batch
- MySQL 8.0, Redis
- JUnit5, Mockito, JMeter

<br />

## _SW Architecture._

![image](https://github.com/user-attachments/assets/a02d128d-5c7a-4927-934f-4fda230a655e)

<br />

## _Feature._

#### 1. 피드백 (좋아요 & 싫어요)

- 피드백: 좋아요/싫어요 데이터가 다량 발생할 것을 고려하여 Redis 에 저장 후, 익일 새벽에 배치 처리를 통해 MySQL 로 이관
- 피드백 배치: 총 4개의 Step 으로 구성하여 자녀 성향에 반영
    - Step1. Redis 에 임시 저장되어 있던 좋아요/싫어요를 MySQL 에 이관
    - Step2. 오늘자 피드백을 읽어 성향에 반영될 점수 계산 및 누적된 성향 변화량 업데이트
    - Step3. 누적 변화량 ≥ 5 일 경우, 자녀 성향 레코드 생성
    - Step4. MBTI 변화가 감지될 경우, 새로운 MBTI 레코드 생성

![image](https://github.com/user-attachments/assets/9cc80f92-bd0d-4b3a-adb9-7355ab1c3bd1)

<br />

#### 2. 추천책 서비스

- Set 자료구조를 활용해 추천 도서 목록 추출 시 중복 저장을 방지
- 대량의 데이터를 페이징 방식으로 분할 처리하여 메모리 사용량을 최소화할 수 있는 JdbcPagingItemReader 사용

![image](https://github.com/user-attachments/assets/5473e261-ac59-4992-a207-a6a7ba9e9b3c)

<br />

#### 3. 도서 검색

- `%`가 문자열 앞에 있을 경우 인덱스 사용이 불가능하므로 Full Table Scan 발생
- 이를 해결하기 위해 토큰 방식의 MySQL Ngram(Full-text Search)을 활용해 도서 검색 최적화
    - 검색 응답시간을 기존 178ms 에서 35ms 로 단축
      ![image](https://github.com/user-attachments/assets/59c1810a-9969-4c69-b80d-8e72328916e3)

<br />

## _Trouble Shooting._

모든 테스트는 **데이터 10만을 기준으로** 진행합니다.

#### 1. 피드백 배치

- 기존 배치: 각 성향별 변화량 확인부터 성향 갱신까지의 step 을 2개로 나누어 복잡한 로직으로 처리 & 성능 저하
- 개선한 배치: **4개로 분리하여 순차적으로 테이블을 업데이트**하도록 변경
    - Step별 테스트 결과: 1m 1s → 6m 6s → 22s → 46s = 8m 5s
    - Job 테스트 결과: 7m 22s
- 개선 필요: 병목 지점인 step2 에서 자녀와 책 성향을 조회하는 쿼리가 하나씩 날아가는 문제 해결
  ![image](https://github.com/user-attachments/assets/ace42950-7299-4e5f-84f5-45da3b742680)

<br />

#### 2. 추천책 배치

- 기존 배치: JPA Identity 전략으로 인한 Bulk Insert 불가능
- 개선한 배치
    - Processor 개선: IO 를 줄일 수 있는 구조로 재설계하여 병목 현상을 줄임
    - Writer 개선: 대량의 데이터를 한꺼번에 삽입후 INSERT 작업을 처리할 수 있도록 **임시 테이블 생성** & **Batch Insert 처리**
    - 배치 처리 시간을 1시간 → 45분 → 17분으로 약 72% 단축시킴

![image](https://github.com/user-attachments/assets/3de338d7-fe47-4e1f-83cb-6d8cc8fc010a)

<br />

## _Test._

![image](https://github.com/user-attachments/assets/7b2a4b80-4266-4754-b8a7-644ac3d2ec1d)
