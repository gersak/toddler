const path = require('path');

module.exports = {
	name: "Toddler",
	entry: './public/js/showcase.js',
	output: {
		filename: './js/main.js',
		path: path.resolve(__dirname,'public')
	},
	resolve: {
		extensions: [".js", ".json", ".jsx", ".css", ".esm.js"]
	}
}
