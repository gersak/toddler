const path = require('path');

module.exports = {
	name: "Toddler",
	entry: './release/js/showcase.js',
	output: {
		filename: './js/main.js',
		path: path.resolve(__dirname,'release')
	},
	resolve: {
		extensions: [".js", ".json", ".jsx", ".css", ".esm.js"]
	}
}
