# student-bsu-by

Cross-platform (Android + iOS) client for [BSU student's personal account](https://student.bsu.by).
The app is unofficial and is not affiliated with Belarusian State University.

This project is a fork of [alexzhirkevich/student-bsu-by](https://github.com/alexzhirkevich/student-bsu-by)
by Alexander Zhirkevich, rewritten with Kotlin Multiplatform. The original Android app is a separate
application, published in [Google Play](https://play.google.com/store/apps/details?id=github.alexzhirkevich.studentbsuby).

## Screenshots

<table>
  <tr>
    <td><img src="/screenshots/screen0.png" width=250></td>
    <td><img src="/screenshots/screen1.png" width=250></td>
    <td><img src="/screenshots/screen2.png" width=250></td>
    <td><img src="/screenshots/screen3.png" width=250></td>
  </tr>
</table>


## Built with kotlin multiplatform (KMP)

Works both on ios and android.

* UI - Compose Multiplatform
* Network - Ktor
* Database - Room
* DI - Koin
* Concurrency - Coroutines, Flow
* Architecture - MVVM

## Release

See [RELEASE.md](RELEASE.md). Privacy policy: [docs/privacy-policy.md](docs/privacy-policy.md).

## Authors

* **Pavel Sushko** - [Telegram](https://t.me/sushkpavel) - multiplatform rework, current fork owner
* **Alexander Zhirkevich** - [Telegram](https://t.me/alexzhirkevich) - original idea

## License

[MIT](LICENSE.md)
