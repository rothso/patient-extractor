# Patient Extractor
> Download and redact patient summaries from Athena.

<p align="center"><img src="extractor.gif?raw=true"/></p>

## Getting Started

Download the [latest build](https://github.com/rothso/patient-extractor/releases/latest) of the 
JAR. Then, create a file named `.env` with the following key-value pairs:

```
ATHENA_KEY=your-api-key
ATHENA_SECRET=your-api-secret
PRACTICE_ID=clinic-id
```

Alternatively, if you're using the practice API, only provide the practice API key/secret and 
leave off the practice ID:
```
ATHENA_KEY=practice-api-key
ATHENA_SECRET=practice-api-secret
```

Run the jar to start the download process:

```sh
> java -jar patient-extractor.jar
```

The redacted HTML summaries are saved in the `encounters` folder.

## Features

You can also optionally specify the maximum number of concurrent requests as the first argument 
(the default is 10 for a real API, 2 for the preview API). The program will automatically reduce 
this number whenever it gets rate-limited.

```sh
# Run with maximum 20 concurrent requests
> java -jar patient-extractor.jar 20
```

If the program crashes or exits, the downloader will resume where it left off the next time you 
run the program. Two files are used to persist state across runs:

* `faker.json`: Stores all Faker associations
* `page.json`: Stores the last-visited page

To completely start over, delete the `encounters` folder and these two files.

## Building from source
You can download and compile this project yourself:

```sh
# Linux
$ git clone https://github.com/rothso/patient-extractor.git
$ cd patient-extractor
$ ./gradlew
$ gradle build
$ gradle jar
```
```sh
# Windows
> git clone https://github.com/rothso/patient-extractor.git
> cd patient-extractor
> gradlew.bat
> gradle build
> gradle jar
```

---
*Built with :heart: for [MASS Free Clinic](http://www.massclinic.org/)*.
