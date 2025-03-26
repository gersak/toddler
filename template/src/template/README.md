# WEB
```
# To start development
npm run dev

# To start build process
# Build will be available in /target folder

npm run release
```


# TAURI



```
cargo install tauri-cli
cargo install tauri-mobile


cargo tauri init
cd src-tauri
```

## DESKTOP
```
cargo tauri dev
```

## TAURI IOS
```
cargo tauri ios init
cargo tauri ios dev
```

## TAURI ANDROID
Prepare development environment
```
adb reverse tcp:8000 tcp:8000
adb reverse tcp:9630 tcp:9630


Start development
```
cargo tauri android init
cargo tauri android dev
```
