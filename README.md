This is a text-based adventure game that walks you through a gripping story about becoming a politician.
Every decision you make shapes how the plot turns out. Choose wisely!

## Running it

    ./mvnw javafx:run

Java 24 or newer. Everything else comes down with Maven.

## Installers

An installer is built on the platform it is for - jpackage cannot cross-build -
so there is one script per platform and a CI matrix that runs all three:

| Platform | Command | Result |
|---|---|---|
| macOS | `./packaging/package.sh` | `target/installer/Fityesz Kronika-1.0.0.dmg` |
| Linux | `./packaging/package.sh` | `target/installer/fityesz-kronika_1.0.0_amd64.deb` |
| Windows | `packaging\package.bat` | `target\installer\Fityesz Kronika-1.0.0.msi` |

`TYPE=app-image` builds just the application folder (no installer), and
`VERSION=1.2.0` sets the version.

The Windows build needs the [WiX Toolset](https://wixtoolset.org) 3.x on PATH;
jpackage uses it to write the .msi.

**All three at once:** run the *Installers* workflow
(`.github/workflows/installers.yml`) from the Actions tab, or push a `v*` tag.
It builds on macOS, Windows and Linux and uploads the three installers as
artifacts.

### Accounts after cloning

`src/main/resources/com/example/demo/supabase.properties` holds the project's
URL and publishable key, and it is **not** in the repository - so a fresh clone
plays fine but cannot sign in: the game has no project to ask. Write the file
once:

    url=https://<project>.supabase.co
    anonKey=<the publishable key>

Both values are public by design - the website hands them to every visitor in
its JavaScript, and row-level security is what protects the data - so they are
safe to pass to whoever works on the game. If the website repo is checked out
next to this one, the file can be generated from its `.env.local`:

    grep -E '^NEXT_PUBLIC_SUPABASE_(URL|ANON_KEY)=' ../fityeszTheGameWebsite/.env.local \
      | sed -e 's/^NEXT_PUBLIC_SUPABASE_URL=/url=/' \
            -e 's/^NEXT_PUBLIC_SUPABASE_ANON_KEY=/anonKey=/' \
      > src/main/resources/com/example/demo/supabase.properties

`FITYESZ_SUPABASE_URL` and `FITYESZ_SUPABASE_ANON_KEY` do the same job from the
environment, and win over the file.

### Accounts in a packaged build

The Supabase URL and anon key live in
`src/main/resources/com/example/demo/supabase.properties`, which is gitignored -
so a fresh clone (and CI) builds a game that plays fine but has no accounts and
no cloud saves. Put the two values there to switch accounts on:

    url=https://<project>.supabase.co
    anonKey=<the anon / publishable key>

The CI workflow writes the same file from the `SUPABASE_URL` and
`SUPABASE_ANON_KEY` repository secrets. Both values are public by design - the
website hands them to every visitor - and row-level security is what protects
the data.
