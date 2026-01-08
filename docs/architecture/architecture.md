# Architecture Diagram

Below is a Mermaid diagram source. Export it to `docs/architecture/architecture.png` using Mermaid Live Editor
or a VS Code Mermaid plugin.

```mermaid
flowchart TB
    UI[Compose UI] --> VM[MainViewModel]
    VM --> PC[PlaybackClient/PlayerController]
    PC --> MS[PlaybackService & MediaSession]
    MS --> P[ExoPlayer]

    VM --> Repo[MusicRepository]
    Repo --> DB[(Room DB)]
    Repo --> MSRC[MediaStore]

    PC --> Notif[Media3 Notification]
    PC --> System[Lockscreen/Bluetooth/Headset]
```

## Export Steps
1. Copy the Mermaid block to https://mermaid.live.
2. Export as PNG.
3. Save it to `docs/architecture/architecture.png`.
