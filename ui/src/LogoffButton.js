import React, {Component} from 'react';
import MessagesLoader from "./MessagesLoader";
import { withRouter } from 'react-router-dom';

class LogoffButton extends Component {
  constructor(props) {
    super(props);
    this.state = {
      messages: MessagesLoader.Empty
    };
  }

  componentDidMount = async() => {
    try {
      this.setState({
        messages: await new MessagesLoader().load([
          { key: 'logoff'}
        ])
      });
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }
  
  logoff = async() => {
    try {
      const resp = await fetch(
        "/logoff", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            "Csrf-Token": "nocheck"
          },
        }
      );
      if (resp.status === 200) {
        this.props.history.push("/");
        if (this.props.onLogoffSuccess !== undefined)
          this.props.onLogoffSuccess();
      } else {
        console.log("error: " + resp.status);
      }
    } catch (e) {
      console.log("Error: " + JSON.stringify(e));
    }
  }

  render() {
    return (
      <button id="logoffButton" className='button' onClick={(e) => this.logoff()}>{this.state.messages('logoff')}</button>
    );
  }
}

export default withRouter(LogoffButton);

