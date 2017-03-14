Starting assumtions:

Java >= 8 is installed, leiningen is installed.

readline wrap is installed.

In chrome dev tools inspector enable custom formatters.

Start with this template:

https://github.com/bhauman/figwheel-template

1. Setup a new project using lein-figwheel template.

```bash
lein new figwheel om-wp -- --om
cd om-wp
```

Before starting figwheel, we'll edit `project.clj`.

1.a. Edit the included Om version to latest release found here:
https://github.com/omcljs/om/releases,
1.0.0-alpha48 as of March 13th.

Add `core.match` to the dependencies vector:

```clojure
[org.clojure/core.match "0.3.0-alpha4"]
```

Comment out
`:open-urls` under `:cljsbuild -> :builds -> "dev" -> :figwheel`
if you don't want a new browser window popping up every time you start
figwheel.

2. Start figwheel:

```bash
rlwrap lein figwheel
```

Navigate a browser to: http://localhost:3449

3. Edit `om-wp/src/om_wp/core.cljs`

Update om.core to om.next.

Require: core.async, sablono, core.match, clojure.string.

Delete template helper code.

Add a stateless component, render root with reconciler.

4. Add a query.

5. Add a mutation with incorrect second type.

6. Fix mutation.

```clojure
(in-ns 'om-wp.core)
@reconciler
```
@reconciler in repl to see that state is updating.

7. Animation.

    git checkout step-7

8. Animation 2.

    git checkout step-8
