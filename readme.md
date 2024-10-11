# Item Components

**Item Components** is a Minecraft mod that allows you to attach custom data to items through "components". These components provide an easy way to modify item behavior and properties using data packs, making it highly customizable and accessible for non-coders.

**Ported to Ignite Loader by Arubik**

## Features

- **Custom Item Components:** Easily attach additional data to items, enhancing functionality through data-driven customization.
- **Data Pack Compatibility:** Utilize data packs to define and manage item components, allowing for quick and flexible modifications without code.
- **Extensible System:** Build your own components through simple JSON configurations, supporting a wide range of modded content and new gameplay mechanics.
- **Developer-Friendly API:** Advanced users can still interact with the mod's API for deeper integration.

## Getting Started

### Installation

1. **Download** the mod from the [Modrinth page](https://modrinth.com/mod/item-components).
2. **Install** the mod by placing the `.jar` file into your Minecraft `mods` folder.
3. Ensure that you're using **Ignite Loader** to run the mod.

### Usage with Data Packs

To use Item Components, you need to create or modify data packs to define custom components for your items. This is done by adding JSON files to the correct folder structure in your data pack.

### Example: Adding a Custom Component

Below is an example of a JSON file that adds a custom component to an item:

```json
{
  "targets": "minecraft:sugar",
  "components": {
    "minecraft:food": {
		"nutrition": 1,
		"saturation": 0.1,
		"eat_seconds": 0.8
	}
  }
}
```

Place this file in your data pack under data/<namespace>/item_components/<item_name>.json.

For more detailed instructions on how to set up components via data packs, refer to the official documentation.

Contributing
Contributions are welcome! If you have ideas, suggestions, or bug reports, feel free to open an issue or submit a pull request on the project's GitHub.

License
This mod is available under the MIT License. See the LICENSE file for more information.