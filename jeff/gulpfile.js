var gulp            = require('gulp'),
    gulpOpen        = require('gulp-open'),
    gutil           = require('gulp-util'),
    stylus          = require('gulp-stylus'),
    minifyCSS       = require('gulp-minify-css'),
    uglify          = require('gulp-uglify'),
    jade            = require('gulp-jade'),
    changed         = require('gulp-changed'),
    cached          = require('gulp-cached'),
    filter          = require('gulp-filter'),
    jadeInheritance = require('gulp-jade-inheritance'),
    autoprefixer    = require('gulp-autoprefixer'),
    sourcemaps      = require('gulp-sourcemaps'),
    concat          = require('gulp-concat'),
    rename          = require("gulp-rename"),
    flatten         = require('gulp-flatten'),
    marked          = require('marked'), // For :markdown filter in jade
    path            = require('path'),
    plumber         = require('gulp-plumber'),
    notify          = require('gulp-notify'),
    livereload      = require('gulp-livereload'),
    tinylr          = require('tiny-lr'),
    express         = require('express'),
    app             = express(),
    server          = tinylr(),
    _if             = require('gulp-if'),
    isWindows       = /^win/.test(require('os').platform());

// --- Stylus ---
gulp.task('stylus', function () {
  gulp.src('./dev/styles/*.styl')
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
  .pipe(gulp.dest('./build/css'))
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
  .pipe(gulp.dest('./dev/styles'));
});

// --- Scripts ---
gulp.task('js', function() {
  return gulp.src([
    './dev/scripts/vendor/*.js',
    './dev/scripts/*.js'
  ])
  .pipe(concat('app.js'))
  // .pipe(uglify() )
  .pipe(gulp.dest('./build/js'))
  .pipe(livereload(server))
  .pipe(_if(!isWindows, notify({
    title: 'Sucess',
    message: 'Javascript compiled'
  })));
});

// --- Jade ---
gulp.task('templates', function() {
  return gulp.src('./dev/**/*.jade')
    .pipe(changed('dist', {extension: '.html'}))
    .pipe(_if(global.isWatching, cached('jade')))
    .pipe(jadeInheritance({basedir: './dev'}))
    .pipe(filter(function (file) {
      return !/\/_/.test(file.path) || !/^_/.test(file.relative);
    }))
    .pipe(jade({
      pretty: true
    }))
    .pipe(gulp.dest('./build'))
    .pipe(livereload(server))
    .pipe(_if(!isWindows, notify({
      title: 'Sucess',
      message: 'Jade compiled'
    })));
});

// --- Server ---
gulp.task('server', function() {
  app.use(require('connect-livereload')());
  app.use(express.static(path.resolve('./build')));
  app.listen(4000);
  gutil.log('Listening on localhost:4000');
});

// --- Open ---
gulp.task('gulpOpen', function(){
  return gulp.src('./build/index.html')
    .pipe(gulpOpen('', {url:'http://localhost:4000'}));
});

// --- Watch ---
gulp.task('watch', function () {
  server.listen(35729, function (err) {
    if (err) {
      return console.log(err);
    }
    gulp.watch('./dev/styles/*.styl',['stylus']);
    gulp.watch('./dev/scripts/*.js',['js']);
    gulp.watch('./dev/*.jade',['templates']);
  });
});

// --- Default task ---
gulp.task('default', ['js', 'rename', 'stylus', 'templates', 'server', 'watch', 'gulpOpen']);
