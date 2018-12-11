# Patient Extractor
Tool for downloading and redacting patient summaries from Athena.


## Getting Started

Download the [latest build](https://github.com/rothso/patient-extractor/releases/latest) of the 
JAR. Then create a file named `.env` with the following key-value pairs inside:

```
ATHENA_KEY=your-api-key
ATHENA_SECRET=your-api-secret
PRACTICE_ID=clinic-id
```

Run the jar to begin scraping:

```sh
> java -jar patient-extractor.jar
```

The output HTML summaries are saved in the `encounters` folder.

## Features

You can also optionally specify the maximum number of current requests as the first argument (The 
default is 10 for a real API, 2 for the preview API). The program will automatically reduce this 
number whenever it gets rate-limited.

```sh
# Run with maximum 20 concurrent requests
> java -jar patient-extractor.jar 20
```

If the program crashes or exits, the downloader will resume where it left off when you rerun the 
program. Two files are used to persist state across runs:

* `faker.json`: Stores all Faker associations
* `page.json`: Stores the last-visited page

#### Building from source
You can edit and compile this project yourself:

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
