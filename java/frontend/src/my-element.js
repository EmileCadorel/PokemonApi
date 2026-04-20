import { LitElement, css, html } from 'lit'
import litLogo from './assets/lit.svg'
import viteLogo from './assets/vite.svg'
import heroImg from './assets/hero.png'

export const Page = Object.freeze({
    LOGIN: "login",
    REGISTER: "register",
    HOME: "home",    
    DISPLAY: "display",
    ERROR: "error"
});

export class MyElement extends LitElement {
    static get properties() {
        return {            
            results: { type: Array },
            page: { type: String },
            pokemonName: {type: String},
            pokemonInfo: {type: Object},
            failed: {type: Boolean},
            errorMessage: {type: String}
        }
    }

    constructor() {
        super()
        this.results = []
        this.pokemonName = "";
        this.failed = false;
        
        this.gotoDefaultPage ();
    }

    gotoDefaultPage () {
        if (this.isLoggedIn ()) {
            this.page = Page.HOME;
            this.user = sessionStorage.getItem ("username");
        } else {
            this.page = Page.LOGIN;
        }        
    }
    
    isLoggedIn() {
        const token = sessionStorage.getItem("jwt")
        const name  = sessionStorage.getItem("username")
        return token && name
    }

    logout () {
        sessionStorage.clear ();
    }
    
    /*!
     * ====================================================================================================
     * ====================================================================================================
     * ===================================          RENDERING          ====================================
     * ====================================================================================================
     * ====================================================================================================
     */
    
    render() {        
        switch (this.page) {
        case Page.LOGIN:
            return this.renderLogin ();
        case Page.HOME:
            return this.renderHome ();
        case Page.DISPLAY:
            return this.renderPokemon ();
        default:
            this.gotoDefaultPage ();            
            return this.render ();
        }
    }

    renderLogin () {
        return html`
        <h2> Login </h2>

        ${this.failed
        ? html`<p class="error">${this.errorMessage}</p>`
        : null}

        <form id="form" @submit=${this.onLoginSubmit}>
        <input placeholder="login" id="login">
        <input placeholder="password" id="password" type="password">
        <button type="submit" @click=${this.onLoginSubmit}>Login</button>
        </form>
        `
    }
    
    renderHome () {
        return html`
        ${this.failed
        ? html`<p class="error">${this.errorMessage}</p>`
        : null}

        <form id="form" @submit=${this.onPokemonSubmit}>
        <input @input=${this.onPokemonInput} placeholder="Search Pokémon" list="suggestions">
        <datalist id="suggestions">
          ${this.results.map(name => html`<option>${name}</option>`)}
        </datalist>
        </form>

        <button type="submit" @click=${this.onLogoutSubmit}>Logout</button>  
    `;    
    }

    renderPokemon() {
        return html`
         <button @click=${() => this.page = Page.HOME}>Back</button>
         <h2>Pokémon details: ${this.pokemonName}</h2>
         <button class="button button-like" @click=${this.onToggleLikePokemon}>                   
          <span>Like</span>           
         </button>         
         
         <img src=${this.pokemonInfo.sprite} alt=${this.pokemonInfo.name}/>
         <ul>
           ${this.pokemonInfo.type.map(name=> html`<li>${name}</li>`)}
         </ul>
         `;         
    }

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          EVENTS          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */

    async onLoginSubmit (e) {        
        e.preventDefault ();

        const login = this.renderRoot.querySelector("#login")
        const password = this.renderRoot.querySelector("#password")
        
        const loginV = login.value;
        const passwordV = password.value;
        
        const response = await fetch("/api/login", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ "login": loginV, "password": passwordV })
        });
        
        if (!response.ok) {
            const err = await response.json();

            this.failed = true;
            this.errorMessage = err.message || "Login failed";

            login.value = "";
            password.value = "";
                        
            return;
        }
        
        const result = await response.json ();
        sessionStorage.setItem ("jwt", result["jwt"]);
        sessionStorage.setItem ("username", result["username"]);
        this.errorMessage = "";
        this.page = Page.HOME;
    }

    async onLogoutSubmit (e) {
        e.preventDefault ();

        sessionStorage.removeItem ("jwt");
        sessionStorage.removeItem ("username");
        this.page = Page.LOGIN;
        this.errorMessage = "";
    }
    
    async onPokemonInput(e) {        
        const value = e.target.value;
        if (value.length < 1) return;

        const jwt = sessionStorage.getItem ("jwt");
        const response = await fetch (`api/pokemon-suggest/${value}`, {
            method: "GET",
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "application/json"
            }
        });
        
        const result = await response.json ();        
        this.results = result ["names"];
    }

    async onPokemonSubmit(e) {
        e.preventDefault ();
        const input = this.renderRoot.querySelector("input");        
        const jwt = sessionStorage.getItem ("jwt");
        
        const response = await fetch (`api/pokemon-info/${input.value}`, {
            method: "GET",            
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "application/json"
            }
        });
        
        if (!response.ok) {
            this.failed = true;
            this.errorMessage = "Please enter a valid Pokémon name";
            this.results = [];
            return;
        }
        
        const result = await response.json ();                
        this.page = Page.DISPLAY;
        this.pokemonName = input.value;
        this.pokemonInfo = result;
        this.pokemonInfo.isLiked = false;


        await this.onPokemonSeekLiked ();                
    }

    async onPokemonSeekLiked () {
        const jwt = sessionStorage.getItem ("jwt");
        const response = await fetch (`api/like/${this.pokemonInfo.id}`, {
            method: "GET",            
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "text/plain"
            }
        });
        
        if (!response.ok) {            
            this.pokemonInfo.isLiked = false;
            return;
        }

        const body = await response.text ();
        console.log (body);
        if (body == "true") {
            this.renderRoot.querySelector(".button-like").classList.add ("liked");    
            this.pokemonInfo.isLiked = true;
        } else {            
            this.pokemonInfo.isLiked = false;
        }
        
        console.log ("LIKE?", this.pokemonInfo);
    }

    async onToggleLikePokemon (e) {
        const jwt = sessionStorage.getItem ("jwt");
        var method = "POST";
        if (this.pokemonInfo.isLiked) {
            method = "DELETE";
        }
        
        const response = await fetch (`api/like/${this.pokemonInfo.id}`, {
            method: method,            
            headers: {
                "Authorization": `Bearer ${jwt}`,
                "Content-Type": "text/plain"
            }
        });

        console.log (response);
        if (!response.ok) {
            this.pokemonInfo.isLiked = false;
            return;
        }

        this.renderRoot.querySelector(".button-like").classList.toggle ("liked");     
        this.pokemonInfo.isLiked = !this.pokemonInfo.isLiked;
    }    

    /*!
     * ====================================================================================================
     * ====================================================================================================
     * =====================================          STYLES          =====================================
     * ====================================================================================================
     * ====================================================================================================
     */
    
    static get styles() {
        return css`
          input { padding: 8px; font-size: 1rem; }
          .error {
            color: red;
          }
.button-like {
    border: 2px solid gray;
    background-color: transparent;
    text-decoration: none;
    padding: 1rem;
    position: relative;
    vertical-align: middle;
    text-align: center;
    display: inline-block;
    border-radius: 3rem;
    color: gray;
    transition: all ease 0.4s;

    span {
        margin-left: 0.5rem;
    }

    .fa,
    span {
        transition: all ease 0.4s;
    }

    &:focus {
        background-color: transparent;

        .fa,
        span {
            color: gray;
        }
    }

    &:hover {
        border-color: red;
        background-color: transparent;

        .fa,
        span {
            color: red;
        }
    }
}

.liked {
    background-color: red;
    border-color: red;

    .fa,
    span {
        color: white;

    }

    &:focus {
        background-color: red;

        .fa,
        span {
            color: white;
        }
    }

    &:hover {
        background-color: red;
        border-color: red;

        .fa,
        span {
            color: white;
        }
    }
}

        `;
    }
}

window.customElements.define('pokemon-search', MyElement)
