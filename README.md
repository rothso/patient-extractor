# Patient Extractor
> Download and redact patient summaries from Athena.

<p align="center"><img src="extractor.gif?raw=true"/></p>

## Getting Started

Download the [latest build](https://github.com/rothso/patient-extractor/releases/latest) of the 
JAR. Then, create a file named `.env` with the following key-value pairs:

```ini
ATHENA_KEY=your-api-key
ATHENA_SECRET=your-api-secret
PRACTICE_ID=clinic-id
```

Alternatively, if you're using the Preview API for testing purposes, only provide the Preview API 
key/secret and 
leave off the practice ID:
```ini
ATHENA_KEY=preview-api-key
ATHENA_SECRET=preview-api-secret
```

Run the jar to start the download process:

```sh
> java -jar patient-extractor.jar
```

You can alternatively download the summaries of only certain patients by specifying their IDs in a 
file, one per line, and providng the file name as the first argument:

```sh
> java -jar patient-extractor.jar patient_ids.txt
```

The redacted HTML summaries are saved in the `encounters` folder, or `encounters-preview` if the 
program was run in Preview Mode.

## Features

#### Recovery

If the program crashes or exits, the downloader will resume where it left off the next time you 
run the program. Several files are used to persist state across runs. To completely start over, delete 
the `encounters` folder and these state files:

* `faker.json`: Stores all Faker associations
* `page.txt`: Stores the last-visited page (if extracting all patients)
* `patient.txt`: Stores the last-visited patient (if extracting by patient IDs)

If the program was run in Preview Mode (using the practice API), these files will be prefixed with
`preview-` in order to keep them separate from production runs. 

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
