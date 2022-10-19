## 1. Setup the environment and make it run.
 All three projects are Sprintboot application.<br>

 Need to setup AWS SNS/SQS/S3 in order to use the async API.<br>

 Make sure to update your <i>application.properties</i> file with your AWS IAM account secrets and region.

 AWS Lambda(Sending email) is optional. Code is in [sendEmailCode.py](./lambda/sendEmailCode.py)

## 2. Project structure and details
Look at the [ReportingSystemArchitecture.pdf](./ReportingSystemArchitecture.pdf)

## 3. Make improvement in the code/system level.
Suggestions:
0. Add new features like update/delete/edit report.
1. Improve sync API performance by using multithreading and sending request concurrently to both services.
2. Use a database instead of hashmap in the ExcelRepositoryImpl.
3. Improve code coverage by adding more tests.
4. Convert sync API into microservices by adding Eureka/Ribbon support.
5. Add pressure tests to benchmark the system.
6. Change MongoDB to DynamoDB.
7. Fix bugs.
8. Make the system more robust by adding fault tolerance such like : DeadLetter Queue, retry, cache, fallback etc.
9. Add security and jwt support.
10. Add more fancy UI using angular/react.
11. Setup your CI/CD pipeline.
12. Add new Services like PNGService, JPEGService etc

