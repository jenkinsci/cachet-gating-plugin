package com.redhat.jenkins.plugins.cachet.pipeline.CachetGatingWaitForResourcesStep
f = namespace(lib.FormTagLib)
f.entry(field: "resources", title: _("Resources:")) {
    f.textbox(value: instance == null ? "" : instance.resources.join('\n'))
}
f.entry(field: "timeLimit", title: "Time limit in seconds:"){
    f.textbox()
}
f.entry(field: "abortWhenTimeExceeded", title: "Abort when time exceeded?"){
    f.checkbox()
}
