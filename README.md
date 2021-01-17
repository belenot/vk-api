# Vk Api

Scrapes users online status by some specified interval and stores to database.

## Local start
Set environment variables vkToken and vkAppId and run application.
```shell script
export vkToken=1234abcd
export vkAppId=1234
./gradlew shadowJar
java -jar build/libs/vk-api-all.jar
```