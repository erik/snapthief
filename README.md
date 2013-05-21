SnapThief
=========

Android app to automatically capture SnapChat pictures/videos.

It's absurdly hacky, and was done purely for my own amusement. I doubt it will
be remotely useful or working on anyone else's phone.

Please don't actually use this. Your friends will hate you and you will
be an all-around awful person.

Usage
-----

*Requires root to work.* Need to grab all of the images SnapChat stores in
its application directory thing. Maybe there's a way to do this without root. I
don't know, I'm terrible at Android development.

Captured images/videos are stored in `/sdcard/snapthief/`, and are named
`<sha512sum of file contents>.<ext>`.

License
-------

Copyright (c) 2013 Erik Price

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the “Software”), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
