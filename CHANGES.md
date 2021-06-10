<H1>Changelog</H1>

<H2>1.1.3</H2>
<p><b>Fixed:</b></p>
<ul>
    <li>Not exited library should be removed</li>
    <li>Select latest version of a library if multiple are present #14 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/14" target="_blank">issue</a></li>
</ul>

<p><b>Feature:</b></p>
<ul>
    <li>Wishes launcher showed IP address in ProgressView</li>
</ul>


<H2>1.1.2</H2>
<p><b>Fixed:</b></p>
<ul>
    <li>Cannot find entity for library with ID LibraryId #11 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/11" target="_blank">issue</a></li>
    <li>FileTooBigException on Target definition #12 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/12" target="_blank">issue</a></li>
    <li>Class in function parameter should be import too #15 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/15" target="_blank">issue</a></li>
</ul>

<p><b>Performance:</b></p>
<ul>
    <li>Speed-up re-build project library</li>
</ul>


<H2>1.1.1</H2>
<p><b>Fixed:</b></p>
<ul>
    <li>Lib inner jar not support by IDEA #8 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/8" target="_blank">issue</a></li>
    <li>Accessibility: Bundle-ClassPath not highest priority #9 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/9" target="_blank">issue</a></li>
    <li>Inner class not resolve as inner, wrong accessibility inspection</li>
    <li>Write access required write action</li>
</ul>


<H2>1.1.0</H2>
<p><b>Breaking Changes:</b></p>
<ul>
    <li>New bundle management, split library by single bundle</li>
    <li>Will add many library start with "Partial: "</li>
</ul>

<p><b>Fixed:</b></p>
<ul>
    <li>Now it can resolve jar in bundle-class-path correct.</li>
    <li>Meta-Inf file will created on facet added</li>
    <li>Bundle dependency resolve wrong #1 <a href="https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/1" target="_blank">issue</a></li>
</ul>

<p><b>New:</b></p>
<ul>
    <li>Some manifest completion</li>
</ul>


<H2>1.0.1</H2>
<p><b>Fixed:</b></p>
<ul>
    <li>Module in same project was required in another module, but not add into dependency tree.</li>
    <li>Module in same project was required in another module, but reexport not resolve. Module reexport bundle not pass to another module #3 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/3))</li>
    <li>Manifest bundle reference in same project, linked to wrong module</li>
    <li>Kotlin's property ext access inspection not work #4 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/4))</li>
    <li>Kotlin project alert kotlin bundle not required #5 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/5))</li>
</ul>


<H2>1.0.0</H2>
<ul>
    <li>Initial project, migrating</li>
</ul>
