const watch = require('node-watch');
const exec = require('child_process').exec;

let compile = function() {
	exec('javac -cp libs\\core.jar;. main.java -d dist', (error, stdout, stderr) => {
		if (error) {
			console.error(`exec error: ${error}`);
			return;
		}
		console.log(`stdout: ${stdout}`);
		console.log(`stderr: ${stderr}`);

		console.log("Finish compile");
	});
}

compile();

watch('./',
	{
		recursive: true,
		filter: function(name) {
			return /\.java/.test(name) && !/node_modules/.test(name);
		}
	},
	function(evt, name) {
		console.log("Watcher triggered");
		if (evt == 'update')
			compile();

		if (evt == 'remove')
			compile();
	}
);
