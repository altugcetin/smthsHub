<div align="center">
  <img src="https://i.imgur.com/sJafO6B.png" alt="Header">
  <p align="center">
    DeluxeHub is the all-in-one hub server solution compacting a large amount of hub essentials into one plugin.
    <br />
    <br />
    <a href="https://wiki.lewisdev.fun/free-resources/deluxehub">Wiki</a>
    ·
    <a href="https://www.spigotmc.org/resources/49425/">SpigotMC</a>
    ·
    <a href="https://discord.lewisdev.fun">Discord</a>
  
  [![Codacy Badge](https://api.codacy.com/project/badge/Grade/0daefdcd09d14086b2f96934d283371e)](https://www.codacy.com/manual/ItzSave/DeluxeHub?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=ItzSave/DeluxeHub&amp;utm_campaign=Badge_Grade)
  </p>

[![Stargazers][stars-shield]][stars-url]
    [![Forks][forks-shield]][forks-url]
    [![Issues][issues-shield]][issues-url]

</div>

## Bedrock form id slots

`floodgate:form` cevaplarında yalnızca form id'si vardır. Her plugin kendi bloğunu kullanır: `slot * 1024 + (sayaç mod 1024)`, slot 1–31. Sonuç `0x7FFF` altındadır. `0x8000` biti proxy formlarına ayrılmıştır.

| Plugin | Slot | Id aralığı | Durum |
| --- | --- | --- | --- |
| smthsHub | 1 | 1024–2047 (`0x0400`–`0x07FF`) | 4.0.2'de uygulandı |
| smthsSMP `BedrockUi` | 2 | 2048–3071 (`0x0800`–`0x0BFF`) | Kod değişmedi. Şu an id'yi 1'den `0x7FFE`'ye kadar sayıyor, her blokla çakışır |
| smthsVaults `BedrockMenus` | — | — | Cumulus API. `floodgate:form` dinlemiyor, `silenceKickListener` yok |
| smthsFriends | — | — | `floodgate:form` yok |

`silenceKickListener` kopyası yalnızca smthsHub'daydı. Aynı kanalda eski kopya kalırsa diğer dinleyicileri silmeye devam eder.

## License

This project is licensed under the GNU General Public License v3.0 License - see the [LICENSE](LICENSE) file for details.

<!-- MARKDOWN LINKS & IMAGES -->
[forks-shield]: https://img.shields.io/github/forks/ItzSave/DeluxeHub.svg?style=for-the-badge
[forks-url]: https://github.com/ItzSave/DeluxeHub/network/members
[stars-shield]: https://img.shields.io/github/stars/ItzSave/DeluxeHub.svg?style=for-the-badge
[stars-url]: https://github.com/ItzSave/DeluxeHub/stargazers
[issues-shield]: https://img.shields.io/github/issues/ItzSave/DeluxeHub.svg?style=for-the-badge
[issues-url]: https://github.com/ItzSave/DeluxeHub/issues
