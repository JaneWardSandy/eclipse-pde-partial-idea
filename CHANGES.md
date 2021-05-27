# Changelog

## 1.1.0

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

## 1.0.1

<p><b>Fixed:</b></p>
<ul>
    <li>Module in same project was required in another module, but not add into dependency tree.</li>
    <li>Module in same project was required in another module, but reexport not resolve. Module reexport bundle not pass to another module #3 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/3))</li>
    <li>Manifest bundle reference in same project, linked to wrong module</li>
    <li>Kotlin's property ext access inspection not work #4 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/4))</li>
    <li>Kotlin project alert kotlin bundle not required #5 ([issue](https://github.com/JaneWardSandy/eclipse-pde-partial-idea/issues/5))</li>
</ul>

## 1.0.0

<ul>
    <li>Initial project, migrating</li>
</ul>
