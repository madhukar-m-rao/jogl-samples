# OpenGL 3.2 Query Highlights

### [gl-320-query-conditional](https://github.com/elect86/jogl-samples/blob/master/jogl-samples/src/tests/gl_320/query/Gl_320_query_conditional.java) :

* conditional rendering with `GL_SAMPLES_PASSED` query
* `glBeginQuery` - `glEndQuery`
* `glBeginConditionalRender` - `glEndConditionalRender`

### [gl-320-query-occlusion](https://github.com/elect86/jogl-samples/blob/master/jogl-samples/src/tests/gl_320/query/Gl_320_query_occlusion.java) :

* `GL_SAMPLES_PASSED` query to get the number of samples (fragments) passed, 84100 means 290x290 pixels


Considerations:

* for occlusion you want to use `GL_ANY_SAMPLES_PASSED`
* `SAMPLES_PASSED` and `PRIMITIVES_GENERATED` are useful for performance analysis
