# Meta folder

This folder contains files related to the dicio-android repository that do not really have something to do with the Android app itself, for example assets used in the README or logo projects.

## The colors used for the logo

- `#006800` dark green foreground
- `#daec21` light green background

## Generating the launcher icon

The launcher icon can be generated using [IconKitchen](https://icon.kitchen). These are the parameters that were used. Things that are different from defaults were highlighted.
- **Icon: Image**, then drop `logo.png` on the area that appears or select it from the filesystem
- Scaling: Center
- Mask: none
- **Effect: Drop shadow**
- **Padding: 17%**
- Background type: Color
- **Background color: `#daec21`** from above
- Texture: None
- Badge: none
- Filename: none (defaults to `ic_launcher`)
- **Shape: Squircle**
- Themed: no

## Generating the STT popup icon

These are the IconKitchen parameters:
- **Icon: Clipart**, then search and select the `record_voice_over` icon
- **Color: `#006800`** from above
- **Filename: `ic_stt_popup`**
- everything else is the same as the launcher icon (starting from *Effect*)
