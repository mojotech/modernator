var gulp            = require('gulp'),
    stylus          = require('gulp-stylus'),
    minifyCSS       = require('gulp-minify-css'),
    autoprefixer    = require('gulp-autoprefixer'),
    sourcemaps      = require('gulp-sourcemaps'),
    concat          = require('gulp-concat'),
    notify          = require('gulp-notify'),
    livereload      = require('gulp-livereload'),
    tinylr          = require('tiny-lr'),
    server          = tinylr(),
    _if             = require('gulp-if'),
    isWindows       = /^win/.test(require('os').platform());

// --- Fonts ---
gulp.task('copyFonts', function() {
  gulp.src('./fonts/**')
  .pipe(gulp.dest('../../resources/public/fonts'));
});

// --- Stylus ---
gulp.task('stylus', function () {
  gulp.src('./styles/*.styl')
  .pipe(stylus())
  .on('error', notify.onError({
    title: 'Fail',
    message: 'Stylus error'
  }))
  .on('error', function (err) {
    return console.log(err);
  })
    .pipe(sourcemaps.init())
    .pipe(autoprefixer({
      browsers: ['last 5 versions']
    }))
    .pipe(sourcemaps.write())
  .pipe(concat("styles.css"))
  .pipe(minifyCSS('styles.css'))
  .pipe(gulp.dest('../../resources/public/css'))
    .pipe(livereload(server))
    .pipe(_if(!isWindows, notify({
      title: 'Sucess',
      message: 'Stylus compiled'
    })));
});

// --- Normalize ---
gulp.task('rename', function() {
  gulp.src([
    'bower_components/**/normalize.css'
  ])
  .pipe(concat('_vendor.styl'))
  .pipe(gulp.dest('./styles'));
});

// --- Watch ---
gulp.task('watch', function () {
  server.listen(35729, function (err) {
    if (err) {
      return console.log(err);
    }
    gulp.watch('./styles/*.styl',['stylus']);
  });
});

// --- Default task ---
gulp.task('default', ['copyFonts', 'stylus', 'rename', 'watch']);
